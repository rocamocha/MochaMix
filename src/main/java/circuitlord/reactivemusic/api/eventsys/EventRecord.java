package circuitlord.reactivemusic.api.eventsys;

import circuitlord.reactivemusic.impl.eventsys.RMEventRecord;
import circuitlord.reactivemusic.impl.eventsys.RMPluginIdentifier;

public interface EventRecord {

    String getEventId();
    RMPluginIdentifier getPluginId();

    static RMEventRecord create(String eventId, RMPluginIdentifier pluginId) {
        return new RMEventRecord(eventId, pluginId);
    }
}
