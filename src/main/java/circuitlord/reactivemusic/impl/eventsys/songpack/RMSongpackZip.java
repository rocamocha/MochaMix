package circuitlord.reactivemusic.impl.eventsys.songpack;

import circuitlord.reactivemusic.api.eventsys.songpack.RuntimeEntry;
import circuitlord.reactivemusic.api.eventsys.songpack.SongpackZip;
import java.nio.file.Path;
import java.util.List;

public class RMSongpackZip implements SongpackZip {

    public RMSongpackConfig config;


    public List<RuntimeEntry> runtimeEntries;


    public Path path;

    public String errorString = "";
    public boolean blockLoading = false;

    // backwards compat
    public boolean convertBiomeToBiomeTag = false;

    public boolean isv05OldSongpack = false;

    public boolean embedded = false;

    public boolean isEmbedded() { return embedded; }
    public RMSongpackConfig getConfig() { return config; }
    public Path getPath() { return path; }
    public String getErrorString() { return errorString; }
    public void setErrorString(String s) { errorString = s; }
    public List<RuntimeEntry> getEntries() { return List.copyOf(runtimeEntries); }
    
    public String getName() { return config.name; }
    public String getAuthor() {return config.author; }

}
