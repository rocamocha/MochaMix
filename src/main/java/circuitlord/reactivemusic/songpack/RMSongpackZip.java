package circuitlord.reactivemusic.songpack;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;

import java.nio.file.Path;
import java.util.List;

public class RMSongpackZip implements SongpackZip {

    public RMSongpackConfig config;


    public List<RMRuntimeEntry> runtimeEntries;


    public Path path;

    public String errorString = "";
    public boolean blockLoading = false;

    // backwards compat
    public boolean convertBiomeToBiomeTag = false;

    public boolean isv05OldSongpack = false;

    public boolean embedded = false;

    public Path getPath() { return path; }
    public String getErrorString() { return errorString; }
    public List<RuntimeEntry> getEntries() { return List.copyOf(runtimeEntries); }

}
