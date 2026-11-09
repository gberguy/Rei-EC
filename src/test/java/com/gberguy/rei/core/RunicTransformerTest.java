package com.gberguy.rei.core;

import com.gberguy.rei.TestPlayers;
import com.gberguy.rei.item.ItemRei;
import com.gberguy.rei.player.FeedingData;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipFile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RunicTransformerTest {
    private static final String TARGET = "thaumcraft.common.lib.events.PlayerEvents";
    private static final String HOOK = "com/gberguy/rei/integration/RunicBonus";

    private static byte[] original() throws Exception {
        try (ZipFile jar = new ZipFile(Paths.get("dev-libs/mods/Thaumcraft-1.12.2-6.1.BETA26.jar").toFile());
             InputStream input = jar.getInputStream(jar.getEntry(TARGET.replace('.', '/') + ".class"))) {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static ClassNode node(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static MethodNode maximum(ClassNode node) {
        return node.methods.stream().filter(method -> method.name.equals("handleRunicArmor")).findFirst().get();
    }

    private static String fingerprint(MethodNode method) {
        Textifier text = new Textifier();
        method.accept(new TraceMethodVisitor(text));
        return text.getText().toString();
    }

    @Test
    public void exactReleasedBinaryIsPatchedOnceAndAllOtherMethodsRemainUnchanged() throws Exception {
        byte[] original = original();
        RunicTransformer transformer = new RunicTransformer();
        byte[] patched = transformer.transform(TARGET, TARGET, original);
        ClassNode before = node(original);
        ClassNode after = node(patched);
        MethodNode maximum = maximum(after);
        long hooks = java.util.Arrays.stream(maximum.instructions.toArray()).filter(instruction ->
                instruction instanceof MethodInsnNode && HOOK.equals(((MethodInsnNode) instruction).owner)).count();
        assertEquals(1, hooks);
        new Analyzer<>(new BasicVerifier()).analyze(after.name, maximum);
        for (int index = 0; index < before.methods.size(); index++) {
            if (!before.methods.get(index).name.equals("handleRunicArmor")) {
                assertEquals(fingerprint(before.methods.get(index)), fingerprint(after.methods.get(index)));
            }
        }
        ClassNode twice = node(transformer.transform(TARGET, TARGET, patched));
        assertEquals(fingerprint(maximum), fingerprint(maximum(twice)));
        assertSame(original, transformer.transform("unrelated", "unrelated", original));
        assertNull(transformer.transform(TARGET, TARGET, null));
    }

    @Test(expected = IllegalStateException.class)
    public void incompatibleMaximumFailsExplicitlyRatherThanSilentlyDisablingTheBonus() throws Exception {
        ClassNode original = node(original());
        for (AbstractInsnNode instruction : maximum(original).instructions.toArray()) {
            if (instruction instanceof FieldInsnNode && ((FieldInsnNode) instruction).name.equals("lastMaxCharge")) {
                ((FieldInsnNode) instruction).name = "changedByAnotherPatch";
            }
        }
        ClassWriter writer = new ClassWriter(0);
        original.accept(writer);
        new RunicTransformer().transform(TARGET, TARGET, writer.toByteArray());
    }

    @Test
    public void releasedMaximumCalculationSupportsTradesRemovalDuplicatesAndTotalsAbove127() throws Exception {
        Method calculate = executableMaximum();
        Class<?> fixture = calculate.getDeclaringClass();
        HashMap<Integer, Integer> maxima = new HashMap<>();
        fixture.getField("lastMaxCharge").set(null, maxima);
        fixture.getField("runicInfo").set(null, new HashMap<Integer, Integer>());
        EntityPlayerMP player = TestPlayers.player();
        player.inventory = new InventoryPlayer(player);
        player.ticksExisted = 20;
        when(player.getEntityId()).thenReturn(1);
        AtomicReference<Float> absorption = new AtomicReference<>(153f);
        when(player.getAbsorptionAmount()).thenAnswer(invocation -> absorption.get());
        doAnswer(invocation -> { absorption.set(invocation.getArgument(0)); return null; }).when(player).setAbsorptionAmount(anyFloat());
        FeedingData.set(player, 120);
        ItemStack rei = infused(new ItemStack(new ItemRei()), 10);
        ItemStack ring = infused(new ItemStack(Items.STICK), 8);
        player.inventory.armorInventory.set(2, infused(new ItemStack(Items.LEATHER_CHESTPLATE), 15));
        TestPlayers.equipment(player, rei, ring);
        calculate.invoke(null, player);
        assertEquals(Integer.valueOf(153), maxima.get(1));
        TestPlayers.equipment(player, rei);
        calculate.invoke(null, player);
        assertEquals(Integer.valueOf(145), maxima.get(1));
        assertEquals(145f, absorption.get(), 0f);
        TestPlayers.equipment(player, ring);
        calculate.invoke(null, player);
        assertEquals(Integer.valueOf(23), maxima.get(1));
        assertEquals(23f, absorption.get(), 0f);
        assertEquals(120, FeedingData.get(player));
        TestPlayers.equipment(player, rei, rei.copy(), ring);
        calculate.invoke(null, player);
        assertEquals(Integer.valueOf(163), maxima.get(1));
        FeedingData.set(player, 127);
        player.inventory.armorInventory.set(2, ItemStack.EMPTY);
        TestPlayers.equipment(player, rei);
        calculate.invoke(null, player);
        assertEquals(Integer.valueOf(137), maxima.get(1));
        EntityPlayerMP buyer = TestPlayers.player();
        buyer.inventory = new InventoryPlayer(buyer);
        buyer.ticksExisted = 20;
        when(buyer.getEntityId()).thenReturn(2);
        FeedingData.set(buyer, 7);
        TestPlayers.equipment(buyer, rei);
        calculate.invoke(null, buyer);
        assertEquals(Integer.valueOf(17), maxima.get(2));
        assertEquals(10, rei.getTagCompound().getByte("TC.RUNIC"));
        assertEquals(127, FeedingData.get(player));
    }

    private static ItemStack infused(ItemStack stack, int charge) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("TC.RUNIC", (byte) charge);
        stack.setTagCompound(tag);
        return stack;
    }

    private static Method executableMaximum() throws Exception {
        ClassNode type = node(new RunicTransformer().transform(TARGET, TARGET, original()));
        MethodNode maximum = maximum(type);
        LabelNode end = null;
        for (AbstractInsnNode instruction : maximum.instructions.toArray()) {
            if (instruction instanceof JumpInsnNode) {
                end = ((JumpInsnNode) instruction).label;
                break;
            }
        }
        assertNotNull(end);
        AbstractInsnNode current = end.getNext();
        while (current != null) {
            AbstractInsnNode next = current.getNext();
            maximum.instructions.remove(current);
            current = next;
        }
        maximum.instructions.add(new InsnNode(Opcodes.RETURN));
        maximum.localVariables.clear();
        maximum.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        for (AbstractInsnNode instruction : maximum.instructions.toArray()) {
            if (instruction instanceof FrameNode) {
                maximum.instructions.remove(instruction);
            }
        }
        type.methods.removeIf(method -> !method.name.equals("handleRunicArmor") && !method.name.equals("getRunicCharge"));
        type.fields.clear();
        type.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "lastMaxCharge", "Ljava/util/HashMap;", null, null));
        type.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "runicInfo", "Ljava/util/HashMap;", null, null));
        type.visibleAnnotations = null;
        type.invisibleAnnotations = null;
        HashMap<String, String> names = new HashMap<>();
        names.put("field_70173_aa", "ticksExisted");
        names.put("field_71071_by", "inventory");
        names.put("field_70460_b", "armorInventory");
        names.put("func_70302_i_", "getSizeInventory");
        names.put("func_70301_a", "getStackInSlot");
        names.put("func_145782_y", "getEntityId");
        names.put("func_110139_bj", "getAbsorptionAmount");
        names.put("func_110149_m", "setAbsorptionAmount");
        names.put("func_77942_o", "hasTagCompound");
        names.put("func_77978_p", "getTagCompound");
        names.put("func_74764_b", "hasKey");
        names.put("func_74771_c", "getByte");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        type.accept(new ClassRemapper(writer, new Remapper() {
            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                return names.getOrDefault(name, name);
            }
            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                return names.getOrDefault(name, name);
            }
        }));
        byte[] bytes = writer.toByteArray();
        ClassLoader loader = new ClassLoader(RunicTransformerTest.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (TARGET.equals(name)) {
                    return defineClass(name, bytes, 0, bytes.length);
                }
                throw new ClassNotFoundException(name);
            }
        };
        return loader.loadClass(TARGET).getMethod("handleRunicArmor", EntityPlayer.class);
    }
}
