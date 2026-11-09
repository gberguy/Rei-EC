package com.gberguy.rei.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.item.ItemStack;

public final class RunicIntegration {
    private static Method itemCharge;

    private RunicIntegration() {
    }

    public static int getItemCharge(ItemStack stack) {
        try {
            if (itemCharge == null) {
                itemCharge = Class.forName("thaumcraft.common.lib.events.PlayerEvents")
                        .getMethod("getRunicCharge", ItemStack.class);
            }
            return (Integer) itemCharge.invoke(null, stack);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException ? exception.getCause() : exception;
            throw new IllegalStateException("Rei could not read Thaumcraft's Runic Shielding value", cause);
        }
    }
}
