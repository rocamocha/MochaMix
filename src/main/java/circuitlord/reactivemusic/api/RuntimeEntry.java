package circuitlord.reactivemusic.api;

import java.util.List;

/** Marker for type-safety without exposing internals.*/
public interface RuntimeEntry {
    /**
     * Not implemented yet.
     * @return External option defined in the yaml config.
     * @see RMRuntimeEntry#setExternalOption(String key, Object value)
     */
    Object getExternalOption(String key);

    List<String> getSongs();
    boolean fallbackAllowed();
    boolean shouldOverlay();
}
