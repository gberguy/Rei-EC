package com.gberguy.rei.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public final class FeedingData {
    private static final String KEY = "rei";
    private static final String BONUS = "feeding";

    private FeedingData() {
    }

    public static int clamp(long value) {
        return (int) Math.max(0L, Math.min(127L, value));
    }

    public static int get(EntityPlayer player) {
        return clamp(player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
                .getCompoundTag(KEY).getLong(BONUS));
    }

    public static int set(EntityPlayer player, long value) {
        if (player.world.isRemote) {
            throw new IllegalArgumentException("Feeding progression can only be changed on the server");
        }
        int bonus = clamp(value);
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagCompound rei = persisted.getCompoundTag(KEY);
        rei.setInteger(BONUS, bonus);
        persisted.setTag(KEY, rei);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        return bonus;
    }
}
