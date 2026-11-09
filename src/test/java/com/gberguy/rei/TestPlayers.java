package com.gberguy.rei;

import baubles.api.cap.IBaublesItemHandler;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.mockito.Mockito;

public final class TestPlayers {
    static {
        Bootstrap.register();
    }

    private TestPlayers() {
    }

    public static EntityPlayerMP player() {
        EntityPlayerMP player = Mockito.mock(EntityPlayerMP.class);
        player.world = Mockito.mock(World.class);
        Mockito.when(player.getEntityData()).thenReturn(new NBTTagCompound());
        Mockito.when(player.getUniqueID()).thenReturn(UUID.randomUUID());
        return player;
    }

    public static void makeClient(EntityPlayerMP player) {
        try {
            java.lang.reflect.Field field = World.class.getField("isRemote");
            field.setAccessible(true);
            field.setBoolean(player.world, true);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    public static IBaublesItemHandler equipment(EntityPlayerMP player, ItemStack... stacks) {
        IBaublesItemHandler handler = Mockito.mock(IBaublesItemHandler.class);
        Mockito.when(player.hasCapability(Mockito.any(), Mockito.isNull())).thenReturn(true);
        Mockito.when(player.getCapability(Mockito.any(), Mockito.isNull())).thenReturn(handler);
        Mockito.when(handler.getSlots()).thenReturn(stacks.length);
        for (int slot = 0; slot < stacks.length; slot++) {
            Mockito.when(handler.getStackInSlot(slot)).thenReturn(stacks[slot]);
        }
        return handler;
    }
}
