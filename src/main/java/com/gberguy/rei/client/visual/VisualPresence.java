package com.gberguy.rei.client.visual;

import com.gberguy.rei.network.VisualPresenceMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VisualPresence {
    private final Map<UUID, VisualPresenceMessage> players = new HashMap<>();

    public void update(VisualPresenceMessage message) {
        if (message.equipped) players.put(message.playerId, message);
        else {
            VisualPresenceMessage previous = players.get(message.playerId);
            if (previous != null && previous.entityId == message.entityId && previous.dimension == message.dimension) players.remove(message.playerId);
        }
    }

    public boolean equipped(UUID id, int entityId, int dimension) {
        VisualPresenceMessage state = players.get(id);
        return state != null && state.equipped && state.entityId == entityId && state.dimension == dimension;
    }

    public void clear() { players.clear(); }
}
