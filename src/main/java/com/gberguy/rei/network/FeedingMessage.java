package com.gberguy.rei.network;

import com.gberguy.rei.Rei;
import com.gberguy.rei.player.FeedingData;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class FeedingMessage implements IMessage {
    private UUID playerId;
    private int bonus;

    public FeedingMessage() {
    }

    public FeedingMessage(UUID playerId, int bonus) {
        this.playerId = playerId;
        this.bonus = FeedingData.clamp(bonus);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getBonus() {
        return bonus;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        playerId = new UUID(buffer.readLong(), buffer.readLong());
        bonus = FeedingData.clamp(buffer.readInt());
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(playerId.getMostSignificantBits());
        buffer.writeLong(playerId.getLeastSignificantBits());
        buffer.writeInt(bonus);
    }

    public static final class Handler implements IMessageHandler<FeedingMessage, IMessage> {
        @Override
        public IMessage onMessage(FeedingMessage message, MessageContext context) {
            Rei.proxy.receive(message, context);
            return null;
        }
    }
}
