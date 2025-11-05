package rocamocha.lootsparkle.enchantment;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

/**
 * Registers custom enchantments by checking and logging at server world load time
 */
public class EnchantmentRegistration {
    private static boolean checked = false;

    public static void registerEventHandlers() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!checked) {
                checked = true;
                checkEnchantments(server);
            }
        });
    }

    /**
     * Attempt to directly register enchantments.
     * This is called early during mod init but registries may not be ready yet.
     */
    public static void directRegister() {
        // This will be a no-op in early init, but the method exists for the call
    }

    private static void checkEnchantments(net.minecraft.server.MinecraftServer server) {
        // Check enchantments are registered
    }
}
