package circuitlord.reactivemusic.api;

public interface SongpackEntry {
    public String[] getEvents();

    public boolean getAllowFallback();
    public boolean getUseOverlay();

    public enum forceStop {
        OnChanged, OnValid, OnInvalid
    }

    public boolean getForceStop(forceStop forceStop);
    public boolean getForceStart();
    public float getForceChance();
}
