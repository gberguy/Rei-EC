package com.gberguy.rei.player;

import baubles.api.BaublesApi;
import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import com.gberguy.rei.item.ItemRei;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class ReiEquipment {
    private ReiEquipment() {
    }

    public static boolean isEquipped(EntityPlayer player) {
        if (player == null || !player.hasCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null)) {
            return false;
        }
        IBaublesItemHandler inventory = BaublesApi.getBaublesHandler(player);
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemRei) {
                return true;
            }
        }
        return false;
    }
}
