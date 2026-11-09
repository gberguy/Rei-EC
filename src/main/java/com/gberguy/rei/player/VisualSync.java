package com.gberguy.rei.player;

import com.gberguy.rei.Rei;
import com.gberguy.rei.network.ReiNetwork;
import com.gberguy.rei.network.VisualPresenceMessage;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = Rei.MOD_ID)
public final class VisualSync {
    private static final Map<EntityPlayerMP, Boolean> PREVIOUS = new WeakHashMap<>();

    private VisualSync() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof EntityPlayerMP && !event.player.world.isRemote) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            boolean equipped = !player.isDead && ReiEquipment.isEquipped(player);
            Boolean previous = PREVIOUS.put(player, equipped);
            if (previous == null || previous != equipped || player.ticksExisted % 100 == 0) ReiNetwork.visual(player, equipped);
        }
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP && event.getTarget() instanceof EntityPlayerMP) {
            EntityPlayerMP target = (EntityPlayerMP) event.getTarget();
            ReiNetwork.visualTo(new VisualPresenceMessage(target, !target.isDead && ReiEquipment.isEquipped(target)),
                    (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public static void stoppedTracking(PlayerEvent.StopTracking event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP && event.getTarget() instanceof EntityPlayerMP) {
            ReiNetwork.visualTo(new VisualPresenceMessage((EntityPlayerMP) event.getTarget(), false),
                    (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        PREVIOUS.remove(event.player);
    }

    @SubscribeEvent
    public static void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        PREVIOUS.remove(event.player);
    }

    @SubscribeEvent
    public static void dimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        PREVIOUS.remove(event.player);
    }

    @SubscribeEvent
    public static void logout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        PREVIOUS.remove(event.player);
    }

    public static void clear() {
        PREVIOUS.clear();
    }
}
