package com.gberguy.rei.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class RunicTransformer implements IClassTransformer {
    private static final String TARGET = "thaumcraft.common.lib.events.PlayerEvents";
    private static final String OWNER = TARGET.replace('.', '/');
    private static final String HOOK = "com/gberguy/rei/integration/RunicBonus";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        ClassNode type = new ClassNode();
        new ClassReader(bytes).accept(type, 0);
        for (MethodNode method : type.methods) {
            if ("handleRunicArmor".equals(method.name)
                    && "(Lnet/minecraft/entity/player/EntityPlayer;)V".equals(method.desc)
                    && (method.access & Opcodes.ACC_STATIC) != 0) {
                patch(method);
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                type.accept(writer);
                LogManager.getLogger("Rei").info("Integrated Rei feeding bonus into Thaumcraft's Runic maximum");
                return writer.toByteArray();
            }
        }
        throw incompatible();
    }

    private static void patch(MethodNode method) {
        FieldInsnNode anchor = null;
        int local = -1;
        int chargeCalls = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (HOOK.equals(call.owner) && "get".equals(call.name)) {
                    return;
                }
                if (OWNER.equals(call.owner) && "getRunicCharge".equals(call.name)
                        && "(Lnet/minecraft/item/ItemStack;)I".equals(call.desc)) {
                    AbstractInsnNode add = nextCode(call);
                    AbstractInsnNode store = nextCode(add);
                    if (add == null || add.getOpcode() != Opcodes.IADD || !(store instanceof VarInsnNode)
                            || store.getOpcode() != Opcodes.ISTORE) {
                        throw incompatible();
                    }
                    int found = ((VarInsnNode) store).var;
                    if (local != -1 && local != found) {
                        throw incompatible();
                    }
                    local = found;
                    chargeCalls++;
                }
            }
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (anchor == null && field.getOpcode() == Opcodes.GETSTATIC && OWNER.equals(field.owner)
                        && "lastMaxCharge".equals(field.name) && "Ljava/util/HashMap;".equals(field.desc)) {
                    if (chargeCalls != 2 || local < 1) {
                        throw incompatible();
                    }
                    anchor = field;
                }
            }
        }
        if (anchor == null || chargeCalls != 2) {
            throw incompatible();
        }
        InsnList addition = new InsnList();
        addition.add(new VarInsnNode(Opcodes.ILOAD, local));
        addition.add(new VarInsnNode(Opcodes.ALOAD, 0));
        addition.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "get",
                "(Lnet/minecraft/entity/player/EntityPlayer;)I", false));
        addition.add(new InsnNode(Opcodes.IADD));
        addition.add(new VarInsnNode(Opcodes.ISTORE, local));
        method.instructions.insertBefore(anchor, addition);
    }

    private static AbstractInsnNode nextCode(AbstractInsnNode instruction) {
        if (instruction == null) {
            return null;
        }
        do {
            instruction = instruction.getNext();
        } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }

    private static IllegalStateException incompatible() {
        return new IllegalStateException("Rei requires Thaumcraft 6.1.BETA26's native Runic maximum calculation; "
                + "another transformation or Thaumcraft version changed the integration point");
    }
}
