package com.gberguy.rei.network;

import com.gberguy.rei.TestPlayers;
import com.gberguy.rei.client.visual.VisualPresence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VisualPresenceMessageTest {
    @Test
    public void snapshotRoundTripKeepsPlayerEntityDimensionAndEquipmentIdentity() {
        EntityPlayerMP player = TestPlayers.player(); player.dimension = -1;
        when(player.getEntityId()).thenReturn(42);
        ByteBuf buffer = Unpooled.buffer();
        try {
            new VisualPresenceMessage(player, true).toBytes(buffer);
            VisualPresenceMessage copy = new VisualPresenceMessage(); copy.fromBytes(buffer);
            assertEquals(player.getUniqueID(), copy.playerId); assertEquals(42, copy.entityId);
            assertEquals(-1, copy.dimension); assertTrue(copy.equipped); assertEquals(0, buffer.readableBytes());
            VisualPresence presence = new VisualPresence(); presence.update(copy);
            assertTrue(presence.equipped(player.getUniqueID(), 42, -1));
            assertFalse(presence.equipped(UUID.randomUUID(), 42, -1));
            assertFalse(presence.equipped(player.getUniqueID(), 43, -1));
            assertFalse(presence.equipped(player.getUniqueID(), 42, 0));
            player.dimension = 0; when(player.getEntityId()).thenReturn(43);
            presence.update(new VisualPresenceMessage(player, true));
            copy.equipped = false; presence.update(copy);
            assertTrue(presence.equipped(player.getUniqueID(), 43, 0));
            presence.clear(); assertFalse(presence.equipped(player.getUniqueID(), 43, 0));
        } finally { buffer.release(); }
    }
}
