package com.gberguy.rei.item;

import com.gberguy.rei.Rei;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Rei.MOD_ID)
public final class ReiItems {
    public static final ItemRei REI = new ItemRei();

    private ReiItems() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(REI);
    }
}
