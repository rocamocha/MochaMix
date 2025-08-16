package circuitlord.reactivemusic.audio;

import circuitlord.reactivemusic.api.RMPlayer;
import circuitlord.reactivemusic.api.RMPlayerManager;
import circuitlord.reactivemusic.api.RMPlayerOptions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class RMPlayerManagerImpl implements RMPlayerManager {
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

