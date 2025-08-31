package circuitlord.reactivemusic.api;

import java.nio.file.Path;
import java.util.List;

public interface SongpackZip {
    Path getPath();
    String getErrorString();
    List<RuntimeEntry> getEntries();
}