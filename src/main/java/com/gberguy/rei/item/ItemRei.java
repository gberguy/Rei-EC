package com.gberguy.rei.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.gberguy.rei.player.ReiEquipment;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemRei extends Item implements IBauble {
    public ItemRei() {
        setRegistryName("rei", "rei");
        setUnlocalizedName("rei.rei");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET;
    }

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        return wearer instanceof EntityPlayer && !ReiEquipment.isEquipped((EntityPlayer) wearer);
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.type == EnumEnchantmentType.WEARABLE
                || super.canApplyAtEnchantingTable(stack, enchantment);
    }
}
