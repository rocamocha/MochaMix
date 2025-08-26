package circuitlord.reactivemusic.songpack;

import java.util.HashMap;

import circuitlord.reactivemusic.api.SongpackEntry;

public class RMSongpackEntry implements SongpackEntry {

    public HashMap<String, Object> entryMap = new HashMap<>();

    // Type safety is enforced in the getter, so that plugin devs have a lower chance of breaking something by accident
    public <T> T get(String key, Class<T> type) {
        Object value = entryMap.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public Object register(String id) {
        return entryMap.putIfAbsent(id, null);
    }

    
    // BUILT-INS:
    // These are kept class-based to improve developer experience
    // when working on the Reactive Music base mod
    //---------------------------------------------------------------------------------
    // expands out into songpack events and biometag events
    public String[] events;
    
    public boolean allowFallback = false;
    public boolean useOverlay = false;
    
    public boolean forceStopMusicOnChanged = false;
    public boolean forceStopMusicOnValid = false;
    public boolean forceStopMusicOnInvalid = false;
    
    public boolean forceStartMusicOnValid = false;
    public float forceChance = 1.0f;
    public boolean startMusicOnEventValid = false;
    
    // deprecated for now
    public boolean stackable = false;
    public String[] songs;
    
    // deprecated
    public boolean alwaysPlay = false;
    public boolean alwaysStop = false;
    
    // getters
    //---------------------------------------------------------------------------------
    public String[] getEvents() { return events; }
    
    public boolean allowFallback() { return allowFallback; }
    public boolean useOverlay() { return useOverlay; }
    
    public float getForceChance() { return forceChance; }
    public boolean getForceStart() { return forceStartMusicOnValid; }
    public boolean getForceStop(forceStop forceStop) {
        switch (forceStop) {
            case OnChanged:
                return forceStopMusicOnChanged;
            case OnValid:
                return forceStopMusicOnValid;
            case OnInvalid:
                return forceStopMusicOnInvalid;
            default:
                return false;
                
        }
    }

}
