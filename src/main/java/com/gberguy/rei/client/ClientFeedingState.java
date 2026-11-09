package com.gberguy.rei.client;

import com.gberguy.rei.player.FeedingData;
import java.util.OptionalInt;
import java.util.UUID;

public final class ClientFeedingState {
    private Object connection;
    private UUID playerId;
    private int bonus;

    public synchronized void update(Object connection, UUID playerId, int value) {
        this.connection = connection;
        this.playerId = playerId;
        this.bonus = FeedingData.clamp(value);
    }

    public synchronized OptionalInt get(Object connection, UUID playerId) {
        return connection != null && this.connection == connection && playerId != null && playerId.equals(this.playerId)
                ? OptionalInt.of(bonus) : OptionalInt.empty();
    }

    public synchronized void clear() {
        connection = null;
        playerId = null;
        bonus = 0;
    }
}
