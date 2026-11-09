package com.gberguy.rei.player;

import com.gberguy.rei.Rei;
import com.gberguy.rei.network.ReiNetwork;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Rei.MOD_ID)
public final class PlayerLifecycle {
    private PlayerLifecycle() {
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.getEntityPlayer().world.isRemote) {
            FeedingData.set(event.getEntityPlayer(), FeedingData.get(event.getOriginal()));
        }
    }

    @SubscribeEvent
    public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        synchronize(event.player);
    }

    @SubscribeEvent
    public static void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        synchronize(event.player);
    }

    @SubscribeEvent
    public static void dimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        synchronize(event.player);
    }

    private static void synchronize(net.minecraft.entity.player.EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ReiNetwork.sync((EntityPlayerMP) player);
        }
    }
}
