package com.gberguy.rei.network;

import com.gberguy.rei.player.FeedingData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ReiNetwork {
    private static SimpleNetworkWrapper channel;

    private ReiNetwork() {
    }

    public static void register() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel("rei");
        channel.registerMessage(FeedingMessage.Handler.class, FeedingMessage.class, 0, Side.CLIENT);
        channel.registerMessage(VisualPresenceMessage.Handler.class, VisualPresenceMessage.class, 1, Side.CLIENT);
    }

    public static void visual(EntityPlayerMP player, boolean equipped) {
        VisualPresenceMessage message = new VisualPresenceMessage(player, equipped);
        channel.sendToAllTracking(message, player);
        channel.sendTo(message, player);
    }

    public static void visualTo(VisualPresenceMessage message, EntityPlayerMP viewer) {
        channel.sendTo(message, viewer);
    }

    public static void sync(EntityPlayerMP player) {
        channel.sendTo(new FeedingMessage(player.getUniqueID(), FeedingData.get(player)), player);
    }
}
