package com.gberguy.rei.network;

import com.gberguy.rei.Rei;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class VisualPresenceMessage implements IMessage {
    public UUID playerId;
    public int entityId;
    public int dimension;
    public boolean equipped;

    public VisualPresenceMessage() {
    }

    public VisualPresenceMessage(EntityPlayer player, boolean equipped) {
        playerId = player.getUniqueID(); entityId = player.getEntityId();
        dimension = player.dimension; this.equipped = equipped;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        playerId = new UUID(buffer.readLong(), buffer.readLong());
        entityId = buffer.readInt(); dimension = buffer.readInt(); equipped = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(playerId.getMostSignificantBits()); buffer.writeLong(playerId.getLeastSignificantBits());
        buffer.writeInt(entityId); buffer.writeInt(dimension); buffer.writeBoolean(equipped);
    }

    public static final class Handler implements IMessageHandler<VisualPresenceMessage, IMessage> {
        @Override
        public IMessage onMessage(VisualPresenceMessage message, MessageContext context) {
            Rei.proxy.receiveVisual(message, context);
            return null;
        }
    }
}
