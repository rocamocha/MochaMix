package circuitlord.reactivemusic.audio;

import circuitlord.reactivemusic.api.RMPlayer;
import circuitlord.reactivemusic.api.RMPlayerManager;
import circuitlord.reactivemusic.api.RMPlayerOptions;
import circuitlord.reactivemusic.api.ReactiveMusicAPI;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RMPlayerManagerImpl implements RMPlayerManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("reactive_music");

    private static final RMPlayerManagerImpl INSTANCE = new RMPlayerManagerImpl();
    public static RMPlayerManager get() { return INSTANCE; }

    private final Map<String, RMPlayerImpl> players = new ConcurrentHashMap<>();
    private final Map<String, Float> groupDuck = new ConcurrentHashMap<>();

    private RMPlayerManagerImpl() {}

    @Override
    public RMPlayer create(String id, RMPlayerOptions opts) {
        if (players.containsKey(id)) throw new IllegalArgumentException("Player id exists: " + id);
        RMPlayerImpl p = new RMPlayerImpl(id, opts, () -> groupDuck.getOrDefault(opts.group(), 1f));
        players.put(id, p);
        if (opts.autostart()) p.play();
        return p;
    }

    @Override public RMPlayer get(String id) { return players.get(id); }

    @Override public void tick() {
        for (RMPlayer player : players.values()) {

            float fp = player.fadePercent();    // current
            float ft = player.fadeTarget();     // target
            int   dur = player.fadeDuration() > 0 ? player.fadeDuration() : 150;
            if (ft < fp) { player.fadingOut(true); }

            if (fp == 0 && player.stopOnFadeOut() && player.fadingOut()) {
                // reached target – run arrival side effects
                LOGGER.info(player.id() + " has stopped on fadeout");
                if (fp == 0f && player.stopOnFadeOut()) player.stop();
                if (fp == 0f && player.resetOnFadeOut()) player.reset();
                player.fadingOut(false);
            }

            float step = (ft > fp ? 1f : -1f) * (1f / dur);
            float next = fp + step;

            // clamp overshoot and bounds
            if ((step > 0 && next >= ft) || (step < 0 && next <= ft)) next = ft;
            if (next < 0f) next = 0f; else if (next > 1f) next = 1f;

            player.setFadePercent(next);
            if (fp != ft) {
                if (fp == 0 && step > 0) {
                    ReactiveMusicAPI.LOGGER.info(player.id() + " is fading in");
                }

                if (fp == 1 && step < 0) {
                    ReactiveMusicAPI.LOGGER.info(player.id() + " is fading out");
                }
            }

        }
    }

    @Override public Collection<RMPlayer> getAll() {
        return Collections.unmodifiableCollection(players.values());
    }

    @Override public Collection<RMPlayer> getByGroup(String group) {
        return players.values().stream()
                .filter(p -> group.equals(p.getGroup()))
                .map(p -> (RMPlayer) p)
                .collect(Collectors.toList()); // use .toList() if you're on Java 16+ / 21
    }

    @Override public void setGroupDuck(String group, float percent) {
        groupDuck.put(group, clamp01(percent));
        players.values().forEach(p -> {
            if (group.equals(p.getGroup())) p.requestGainRecompute(); // make requestGainRecompute() package-private in RMPlayerImpl
        });
    }

    @Override public float getGroupDuck(String group) {
        return groupDuck.getOrDefault(group, 1f);
    }

    @Override public void closeAllForPlugin(String namespace) {
        players.values().removeIf(p -> {
            boolean match = namespace.equals(p.getNamespace()); // add getNamespace() to RMPlayerImpl
            if (match) p.close();
            return match;
        });
    }

    @Override public void closeAll() {
        players.values().forEach(RMPlayer::close);
        players.clear();
        groupDuck.clear();
    }

    private static float clamp01(float f){ return f < 0 ? 0 : Math.min(f, 1); }
}

