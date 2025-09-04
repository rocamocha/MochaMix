package circuitlord.reactivemusic.api.eventsys.songpack;

import java.util.List;

import circuitlord.reactivemusic.impl.eventsys.songpack.entries.RMEntryCondition;
import circuitlord.reactivemusic.impl.eventsys.songpack.entries.RMRuntimeEntry;

/** Marker for type-safety without exposing internals.*/
public interface RuntimeEntry {
    /**
     * Not implemented yet. TODO: Second parse of the yaml?
     * The dynamic keys can't be typecast beforehand, so we need to get them as a raw map.
     * @return External option defined in the yaml config.
     * @see RMRuntimeEntry#setExternalOption(String key, Object value)
     */
    Object getExternalOption(String key);

    String getEventString();
    String getErrorString();
    List<String> getSongs();
    boolean fallbackAllowed();
    boolean shouldOverlay();
    float getForceChance();
    boolean getForceStopMusicOnValid();
    boolean getForceStopMusicOnInvalid();
    boolean getForceStartMusicOnValid();
    float getCachedRandomChance();
    void setCachedRandomChance(float c);
    String getSongpack();

    List<RMEntryCondition> getConditions();
}
