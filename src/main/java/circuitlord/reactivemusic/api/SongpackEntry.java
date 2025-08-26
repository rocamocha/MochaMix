package circuitlord.reactivemusic.api;

public interface SongpackEntry {
    public <T> T get(String key, Class<T> type);
    public String[] getEvents();

    public boolean allowFallback();
    public boolean useOverlay();

    public enum forceStop {
        OnChanged, OnValid, OnInvalid
    }

    public boolean getForceStop(forceStop forceStop);
    public boolean getForceStart();
    public float getForceChance();
}
