package betterquesting.api.events;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.UUID;

import cpw.mods.fml.common.eventhandler.Event;

public class MarkDirtyPlayerEvent extends Event {

    private final Collection<UUID> dirtyPlayerIDs;

    public MarkDirtyPlayerEvent(UUID dirtyPlayerID) {
        this.dirtyPlayerIDs = Collections.singleton(dirtyPlayerID);
    }

    public MarkDirtyPlayerEvent(Collection<UUID> dirtyPlayerIDs) {
        this.dirtyPlayerIDs = Collections.unmodifiableCollection(new TreeSet<>(dirtyPlayerIDs));
    }

    public Collection<UUID> getDirtyPlayerIDs() {
        return dirtyPlayerIDs;
    }
}
