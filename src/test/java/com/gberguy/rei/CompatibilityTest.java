package com.gberguy.rei;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;
import net.minecraftforge.fml.relauncher.Side;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import static org.junit.Assert.*;

public class CompatibilityTest {
    @Test
    public void missingOrMismatchedRemoteReiIsRejectedOnBothSides() {
        Rei mod = new Rei();
        for (Side side : Side.values()) {
            assertFalse(mod.acceptsRemote(Collections.emptyMap(), side));
            assertFalse(mod.acceptsRemote(Collections.singletonMap("rei", "other"), side));
            assertTrue(mod.acceptsRemote(Collections.singletonMap("rei", Rei.VERSION), side));
        }
    }

    @Test
    public void bothSuppliedBaublesImplementationsProvideTheUsedApiAndValidateCanEquip() throws Exception {
        for (String name : new String[] {"Baubles-1.12-1.5.2.jar", "BaublesEX-1.12.2-2.3.7.jar"}) {
            try (ZipFile zip = new ZipFile(Paths.get("dev-libs/mods", name).toFile())) {
                ClassNode api = read(zip, "baubles/api/BaublesApi");
                assertTrue(hasMethod(api, "getBaublesHandler", "(Lnet/minecraft/entity/player/EntityPlayer;)Lbaubles/api/cap/IBaublesItemHandler;"));
                ClassNode bauble = read(zip, "baubles/api/IBauble");
                assertTrue(hasMethod(bauble, "getBaubleType", "(Lnet/minecraft/item/ItemStack;)Lbaubles/api/BaubleType;"));
                assertTrue(hasMethod(bauble, "canEquip", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)Z"));
                ClassNode container = read(zip, "baubles/api/cap/BaublesContainer");
                MethodNode validator = container.methods.stream().filter(method -> method.name.equals("isItemValidForSlot")).findFirst().get();
                assertTrue(java.util.Arrays.stream(validator.instructions.toArray()).anyMatch(instruction ->
                        instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("baubles/api/IBauble")
                                && ((MethodInsnNode) instruction).name.equals("canEquip")));
            }
        }
    }

    @Test
    public void commonClassesHaveNoClientOrThaumcraftTypeDependencies() throws Exception {
        Path root = Paths.get("build/classes/java/main");
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.filter(file -> file.toString().endsWith(".class"))::iterator) {
                String name = root.relativize(path).toString().replace('\\', '/');
                if (name.startsWith("com/gberguy/rei/client/")) {
                    continue;
                }
                ClassNode type = new ClassNode();
                new ClassReader(Files.readAllBytes(path)).accept(type, 0);
                Set<String> references = new HashSet<>();
                references.add(type.superName);
                references.addAll(type.interfaces);
                for (FieldNode field : type.fields) {
                    addType(references, Type.getType(field.desc));
                }
                for (MethodNode method : type.methods) {
                    addType(references, Type.getReturnType(method.desc));
                    for (Type argument : Type.getArgumentTypes(method.desc)) {
                        addType(references, argument);
                    }
                    for (AbstractInsnNode instruction : method.instructions.toArray()) {
                        if (instruction instanceof MethodInsnNode) {
                            MethodInsnNode call = (MethodInsnNode) instruction;
                            references.add(call.owner);
                            addType(references, Type.getReturnType(call.desc));
                            for (Type argument : Type.getArgumentTypes(call.desc)) {
                                addType(references, argument);
                            }
                        } else if (instruction instanceof FieldInsnNode) {
                            FieldInsnNode field = (FieldInsnNode) instruction;
                            references.add(field.owner);
                            addType(references, Type.getType(field.desc));
                        } else if (instruction instanceof TypeInsnNode) {
                            references.add(((TypeInsnNode) instruction).desc);
                        } else if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof Type) {
                            addType(references, (Type) ((LdcInsnNode) instruction).cst);
                        }
                    }
                }
                for (String reference : references) {
                    assertFalse(type.name + " links to " + reference, reference != null
                            && (reference.startsWith("net/minecraft/client/") || reference.startsWith("thaumcraft/")));
                }
            }
        }
    }

    @Test
    public void nativeInfusionAcceptsBaublesAndCopiesItsInput() throws Exception {
        try (ZipFile zip = new ZipFile("dev-libs/mods/Thaumcraft-1.12.2-6.1.BETA26.jar")) {
            ClassNode recipe = read(zip, "thaumcraft/common/lib/crafting/InfusionRunicAugmentRecipe");
            MethodNode match = recipe.methods.stream().filter(method -> method.name.equals("matches")).findFirst().get();
            assertTrue(java.util.Arrays.stream(match.instructions.toArray()).anyMatch(instruction -> instruction instanceof TypeInsnNode
                    && instruction.getOpcode() == Opcodes.INSTANCEOF && ((TypeInsnNode) instruction).desc.equals("baubles/api/IBauble")));
            MethodNode output = recipe.methods.stream().filter(method -> method.name.equals("getRecipeOutput")).findFirst().get();
            assertTrue(java.util.Arrays.stream(output.instructions.toArray()).anyMatch(instruction -> instruction instanceof MethodInsnNode
                    && ((MethodInsnNode) instruction).name.equals("func_77946_l")));
        }
    }

    private static boolean hasMethod(ClassNode type, String name, String descriptor) {
        return type.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor));
    }

    private static ClassNode read(ZipFile zip, String name) throws Exception {
        try (InputStream input = zip.getInputStream(zip.getEntry(name + ".class"))) {
            ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, 0);
            return result;
        }
    }

    private static void addType(Set<String> references, Type type) {
        if (type.getSort() == Type.OBJECT) {
            references.add(type.getInternalName());
        } else if (type.getSort() == Type.ARRAY) {
            addType(references, type.getElementType());
        }
    }
}
