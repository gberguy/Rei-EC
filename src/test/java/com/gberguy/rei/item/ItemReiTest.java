package com.gberguy.rei.item;

import baubles.api.BaubleType;
import com.gberguy.rei.TestPlayers;
import com.gberguy.rei.integration.RunicBonus;
import com.gberguy.rei.player.FeedingData;
import java.util.Arrays;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemReiTest {
    @Test
    public void wildcardIsRemovableEnchantableAndRejectsDuplicateEquipment() {
        EntityPlayerMP player = TestPlayers.player();
        ItemRei item = new ItemRei();
        ItemStack rei = new ItemStack(item);
        TestPlayers.equipment(player, ItemStack.EMPTY);
        assertEquals(1, item.getItemStackLimit(rei));
        assertEquals(BaubleType.TRINKET, item.getBaubleType(rei));
        assertTrue(item.canEquip(rei, player));
        assertTrue(item.canUnequip(rei, player));
        assertTrue(item.getItemEnchantability() > 0);
        ItemStack[] expanded = new ItemStack[30];
        Arrays.fill(expanded, ItemStack.EMPTY);
        expanded[29] = rei;
        TestPlayers.equipment(player, expanded);
        assertFalse(item.canEquip(new ItemStack(item), player));
    }

    @Test
    public void tradeAndForcedDuplicatesNeverWriteFeedingToItemsOrMultiplyIt() {
        EntityPlayerMP first = TestPlayers.player();
        EntityPlayerMP second = TestPlayers.player();
        FeedingData.set(first, 120);
        FeedingData.set(second, 7);
        ItemStack rei = new ItemStack(new ItemRei());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("TC.RUNIC", (byte) 10);
        tag.setString("custom", "keep");
        rei.setTagCompound(tag);
        NBTTagCompound original = rei.serializeNBT();
        TestPlayers.equipment(first, rei, rei.copy());
        assertEquals(120, RunicBonus.get(first));
        assertEquals(130, tag.getByte("TC.RUNIC") + RunicBonus.get(first));
        TestPlayers.equipment(first, ItemStack.EMPTY);
        TestPlayers.equipment(second, rei);
        assertEquals(0, RunicBonus.get(first));
        assertEquals(7, RunicBonus.get(second));
        assertEquals(120, FeedingData.get(first));
        assertEquals(original, rei.serializeNBT());
        TestPlayers.makeClient(second);
        assertEquals(0, RunicBonus.get(second));
    }

    @Test
    public void missingEquipmentCapabilityHasNoBonus() {
        EntityPlayerMP player = TestPlayers.player();
        FeedingData.set(player, 127);
        assertEquals(0, RunicBonus.get(player));
        assertEquals(0, RunicBonus.get(null));
    }
}
