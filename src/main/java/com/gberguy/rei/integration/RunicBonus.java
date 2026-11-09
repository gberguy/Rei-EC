package com.gberguy.rei.integration;

import com.gberguy.rei.player.FeedingData;
import com.gberguy.rei.player.ReiEquipment;
import net.minecraft.entity.player.EntityPlayer;

public final class RunicBonus {
    private RunicBonus() {
    }

    public static int get(EntityPlayer player) {
        return player != null && !player.world.isRemote && ReiEquipment.isEquipped(player)
                ? FeedingData.get(player) : 0;
    }
}
