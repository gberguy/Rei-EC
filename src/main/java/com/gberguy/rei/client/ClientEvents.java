package com.gberguy.rei.client;

import com.gberguy.rei.integration.RunicIntegration;
import com.gberguy.rei.item.ItemRei;
import com.gberguy.rei.item.ReiItems;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ClientEvents {
    @SubscribeEvent
    public void models(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(ReiItems.REI, 0, new ModelResourceLocation("rei:rei", "inventory"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        EntityPlayer viewer = event.getEntityPlayer();
        if (!(stack.getItem() instanceof ItemRei) || viewer == null) {
            return;
        }
        OptionalInt bonus = ClientProxy.FEEDING.get(Minecraft.getMinecraft().getConnection(), viewer.getUniqueID());
        if (!bonus.isPresent()) {
            event.getToolTip().add(TextFormatting.GRAY + I18n.format("tooltip.rei.awaiting"));
            return;
        }
        event.getToolTip().add(TextFormatting.AQUA + I18n.format("tooltip.rei.feeding", bonus.getAsInt()));
        if (Loader.isModLoaded("thaumcraft")) {
            int infused = RunicIntegration.getItemCharge(stack);
            if (infused <= 0) {
                event.getToolTip().add(TextFormatting.GOLD + I18n.format("tooltip.rei.runic", infused));
            }
            event.getToolTip().add(TextFormatting.GOLD + I18n.format("tooltip.rei.total", (long) infused + bonus.getAsInt()));
        } else {
            event.getToolTip().add(TextFormatting.GRAY + I18n.format("tooltip.rei.no_thaumcraft"));
        }
    }
}
