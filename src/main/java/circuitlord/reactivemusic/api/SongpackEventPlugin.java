package circuitlord.reactivemusic.api;

import circuitlord.reactivemusic.ReactiveMusicState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

/**
 * Your plugin class should implement this interface, as it hooks into the flow of Reactive Music's core programming.
 * For your plugin to be recognized and loaded by Reactive Music, create a plaintext file with the class' full
 * package path (ex. <code>circuitlord.reactivemusic.plugins.WeatherAltitudePlugin</code>) on it's own line in
 * <code>resources/META-INF/services</code> 
 */
public interface SongpackEventPlugin {
    /**
     * This is being moved to a more proper registry object.
     * @return String... meant to serve as the identifier.
     */
    @Deprecated default String getId() { return "PLUGIN ID IS NOT SET!"; }

    static ReactivePluginId registerPlugin(String namespace, String path) {

        ReactivePluginId pluginId = new ReactivePluginId(namespace, path);
        // TODO: Register to a singleton map.
        return pluginId;
        
    }

    /**
     * Identifier class for the plugin registry.
     * Will be set as the value within the registry's map.
     */
    public class ReactivePluginId {

        private String title;
        private String namespace;
        private String path;

        public ReactivePluginId(String ns, String p) {
            this.namespace = ns;
            this.path = p;
        }

        public String getTitle() {
            if (title == null) {
                return "Please set a title for " + namespace + ":" + path;
            }
            return title;
        }

        public String getNamespace() { return namespace; }
        public String getPath() { return path; }

    }

    /** 
     * Freezes the plugin, disallowing its tick functions to be called from Reactive Music's newTick().
     * Override if you want to disallow your plugin's logic being stopped from outside calls.
     * This is 100% not recommended for future-proofing compatibility with other plugins.
     * But sure, if you want to force everyone else to work around your code, sure then - go for it.
     */
    default void freeze() { ReactiveMusicState.logicFreeze.put(getId(), true); }

    /**
     * Removes the plugin from the frozen state, allowing its tick functions to be called from Reactive Music's newTick() again.
     */
    default void unfreeze() { ReactiveMusicState.logicFreeze.put(getId(), false); }

    /**
     * Called during ModInitialize()
     * <p>Use this method to register your new events to the Reactive Music event system.
     * Songpack creators can use these events in their YAML files, it is up to the logic in
     * the overrideable tick methods to set the event states.</p>
     * 
     * @see #tickSchedule()
     * @see #gameTick(PlayerEntity, World, Map)
     * @see #newTick()
     * @see #onValid(RMRuntimeEntry)
     * @see #onInvalid(RMRuntimeEntry)
     */
     default void init() {}
    
    /**
     * Override this method to set a different schedule, or to schedule dynamically.
     * @return The number of ticks that must pass before gameTick() is called each loop.
     */
    default int tickSchedule() { return 20; } // per-plugin configurable tick throttling

    /**
     * Called when scheduled. Default schedule is 20 ticks, and can be configured.
     * Provides player, world, and Reactive Music's eventMap for convenience.
     * @param player
     * @param world
     * @param eventMap
     * @see #tickSchedule()
     */
    default void gameTick(PlayerEntity player, World world, Map<SongpackEvent, Boolean> eventMap) {};

    /** 
     * Called every tick.
     */
    default void newTick() {}

    /**
     * FIXME: Why isn't this getting called??? Help!
     * Calls when <code>entry</code> flips from invalid -> valid.
     * @param entry
     */
    default void onValid(RuntimeEntry entry) {}
    
    /**
     * FIXME: Why isn't this getting called??? Help!
     * Calls when <code>entry</code> flips from valid -> invalid.
     * @param entry
     */
    default void onInvalid(RuntimeEntry entry) {}
}

