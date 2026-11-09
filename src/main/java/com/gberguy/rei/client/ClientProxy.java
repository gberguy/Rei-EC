package com.gberguy.rei.client;

import com.gberguy.rei.proxy.CommonProxy;
import com.gberguy.rei.network.FeedingMessage;
import com.gberguy.rei.network.VisualPresenceMessage;
import com.gberguy.rei.client.visual.CompanionRenderer;
import com.gberguy.rei.client.visual.CompanionVisuals;
import com.gberguy.rei.client.visual.VisualSettings;
import java.io.File;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ClientProxy extends CommonProxy {
    public static final ClientFeedingState FEEDING = new ClientFeedingState();

    private CompanionVisuals visuals;

    @Override
    public void initialize() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        CompanionRenderer renderer = new CompanionRenderer();
        visuals = new CompanionVisuals(renderer, VisualSettings.load(new File(Loader.instance().getConfigDir(), "rei-visuals.cfg")));
        MinecraftForge.EVENT_BUS.register(visuals);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(renderer);
    }

    @Override
    public void receiveVisual(VisualPresenceMessage message, MessageContext context) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            if (minecraft.getConnection() == context.netHandler && minecraft.world != null
                    && minecraft.world.provider.getDimension() == message.dimension) visuals.update(message);
        });
    }

    @Override
    public void receive(FeedingMessage message, MessageContext context) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            if (minecraft.getConnection() == context.netHandler) {
                FEEDING.update(context.netHandler, message.getPlayerId(), message.getBonus());
            }
        });
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        FEEDING.clear();
        Minecraft.getMinecraft().addScheduledTask(() -> { if (visuals != null) visuals.clear(); });
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        FEEDING.clear();
        Minecraft.getMinecraft().addScheduledTask(() -> { if (visuals != null) visuals.clear(); });
    }
}
