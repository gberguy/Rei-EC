package com.gberguy.rei.player;

import com.gberguy.rei.TestPlayers;
import com.gberguy.rei.item.ItemRei;
import com.gberguy.rei.network.ReiNetwork;
import com.gberguy.rei.network.VisualPresenceMessage;
import java.lang.reflect.Field;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VisualSyncTest {
    private SimpleNetworkWrapper channel;
    private Object original;
    private Field field;

    @Before
    public void setup() throws Exception {
        field = ReiNetwork.class.getDeclaredField("channel"); field.setAccessible(true); original = field.get(null);
        channel = mock(SimpleNetworkWrapper.class); field.set(null, channel); VisualSync.clear();
    }

    @After
    public void finish() throws Exception { field.set(null, original); VisualSync.clear(); }

    @Test
    public void ownerAndTrackingClientsReceiveChangesButForcedDuplicatesAreOneCompanion() {
        EntityPlayerMP player = TestPlayers.player(); player.ticksExisted = 1;
        ItemStack rei = new ItemStack(new ItemRei());
        TestPlayers.equipment(player, ItemStack.EMPTY); tick(player);
        TestPlayers.equipment(player, rei); tick(player);
        TestPlayers.equipment(player, rei, rei.copy()); tick(player);
        TestPlayers.equipment(player, ItemStack.EMPTY); tick(player);
        ArgumentCaptor<VisualPresenceMessage> packets = ArgumentCaptor.forClass(VisualPresenceMessage.class);
        verify(channel, times(3)).sendTo(packets.capture(), eq(player));
        verify(channel, times(3)).sendToAllTracking(any(VisualPresenceMessage.class), eq(player));
        assertFalse(packets.getAllValues().get(0).equipped);
        assertTrue(packets.getAllValues().get(1).equipped);
        assertFalse(packets.getAllValues().get(2).equipped);
    }

    @Test
    public void lateObserversReceiveAuthoritativeStateAndStopTrackingRemovesOnlyTheirVisual() {
        EntityPlayerMP target = TestPlayers.player(); EntityPlayerMP viewer = TestPlayers.player();
        TestPlayers.equipment(target, new ItemStack(new ItemRei()));
        VisualSync.tracking(new PlayerEvent.StartTracking(viewer, target));
        VisualSync.stoppedTracking(new PlayerEvent.StopTracking(viewer, target));
        ArgumentCaptor<VisualPresenceMessage> packets = ArgumentCaptor.forClass(VisualPresenceMessage.class);
        verify(channel, times(2)).sendTo(packets.capture(), eq(viewer));
        assertEquals(target.getUniqueID(), packets.getAllValues().get(0).playerId);
        assertTrue(packets.getAllValues().get(0).equipped); assertFalse(packets.getAllValues().get(1).equipped);
        assertTrue(ReiEquipment.isEquipped(target));
    }

    @Test
    public void dimensionRespawnAndLoginResendEvenWhenEquipmentDidNotChange() {
        EntityPlayerMP player = TestPlayers.player(); player.ticksExisted = 1;
        TestPlayers.equipment(player, new ItemStack(new ItemRei()));
        tick(player);
        VisualSync.dimension(new net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent(player, 0, -1)); tick(player);
        VisualSync.respawn(new net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent(player, false)); tick(player);
        VisualSync.logout(new net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent(player));
        VisualSync.login(new net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent(player)); tick(player);
        verify(channel, times(4)).sendTo(any(VisualPresenceMessage.class), eq(player));
        verify(channel, times(4)).sendToAllTracking(any(VisualPresenceMessage.class), eq(player));
        TestPlayers.makeClient(player); tick(player);
        verifyNoMoreInteractions(channel);
    }

    private static void tick(EntityPlayerMP player) { VisualSync.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player)); }
}
