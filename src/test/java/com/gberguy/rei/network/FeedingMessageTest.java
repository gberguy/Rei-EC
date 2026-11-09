package com.gberguy.rei.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FeedingMessageTest {
    @Test
    public void roundTripKeepsPlayerIdentityAndClampsValues() {
        UUID player = UUID.randomUUID();
        ByteBuf buffer = Unpooled.buffer();
        try {
            new FeedingMessage(player, 120).toBytes(buffer);
            FeedingMessage decoded = new FeedingMessage();
            decoded.fromBytes(buffer);
            assertEquals(player, decoded.getPlayerId());
            assertEquals(120, decoded.getBonus());
            assertEquals(0, buffer.readableBytes());
            buffer.clear();
            buffer.writeLong(player.getMostSignificantBits());
            buffer.writeLong(player.getLeastSignificantBits());
            buffer.writeInt(Integer.MAX_VALUE);
            decoded.fromBytes(buffer);
            assertEquals(127, decoded.getBonus());
        } finally {
            buffer.release();
        }
    }
}
