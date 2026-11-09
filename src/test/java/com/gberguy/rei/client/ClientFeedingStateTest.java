package com.gberguy.rei.client;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClientFeedingStateTest {
    @Test
    public void unsynchronizedAndOtherPlayersHaveNoDisplayedValue() {
        ClientFeedingState state = new ClientFeedingState();
        Object connection = new Object();
        UUID player = UUID.randomUUID();
        assertFalse(state.get(connection, player).isPresent());
        state.update(connection, player, 120);
        assertEquals(120, state.get(connection, player).getAsInt());
        assertFalse(state.get(connection, UUID.randomUUID()).isPresent());
        assertFalse(state.get(new Object(), player).isPresent());
    }

    @Test
    public void disconnectAndSinglePlayerSwitchDiscardPreviousProgression() {
        ClientFeedingState state = new ClientFeedingState();
        Object first = new Object();
        Object second = new Object();
        UUID player = UUID.randomUUID();
        state.update(first, player, 127);
        state.clear();
        assertFalse(state.get(first, player).isPresent());
        assertFalse(state.get(second, player).isPresent());
        state.update(second, player, 7);
        assertEquals(7, state.get(second, player).getAsInt());
        assertFalse(state.get(first, player).isPresent());
    }
}
