package circuitlord.reactivemusic.api;

import java.util.List;

import circuitlord.reactivemusic.entries.RMEntryCondition;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;

/** Marker for type-safety without exposing internals.*/
public interface RuntimeEntry {
    /**
     * Not implemented yet. TODO: Second parse of the yaml?
     * The dynamic keys can't be typecast beforehand, so we need to get them as a raw map.
     * @return External option defined in the yaml config.
     * @see RMRuntimeEntry#setExternalOption(String key, Object value)
     */
    Object getExternalOption(String key);

    List<String> getSongs();
    boolean fallbackAllowed();
    boolean shouldOverlay();

    List<RMEntryCondition> getConditions();
}
