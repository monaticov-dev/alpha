package com.alpha;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Battle-test transformer: injects a one-shot log line at the head of
 * Profiler.startSection (MCP, dev) / func_76320_a (SRG, obfuscated runs).
 * Profiler exists on client and server, so both runtimes exercise this.
 * Any failure returns the original bytes (never breaks the launch).
 */
public class AlphaTransformer implements IClassTransformer, Opcodes {
    /** One-shot guard, set from injected bytecode (must be public static). */
    public static boolean logged;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"net.minecraft.profiler.Profiler".equals(transformedName)) {
            return basicClass;
        }
        try {
            ClassNode cn = new ClassNode();
            new ClassReader(basicClass).accept(cn, 0);
            boolean touched = false;
            for (Object o : cn.methods) {
                MethodNode m = (MethodNode) o;
                if (!"(Ljava/lang/String;)V".equals(m.desc)) {
                    continue;
                }
                if (!"startSection".equals(m.name) && !"func_76320_a".equals(m.name)) {
                    continue;
                }
                LabelNode skip = new LabelNode(new Label());
                InsnList ins = new InsnList();
                ins.add(new FieldInsnNode(GETSTATIC, "com/alpha/AlphaTransformer", "logged", "Z"));
                ins.add(new JumpInsnNode(IFNE, skip));
                ins.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                ins.add(new LdcInsnNode("[Alpha] Profiler transformer live"));
                ins.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                        "(Ljava/lang/String;)V", false));
                ins.add(new InsnNode(ICONST_1));
                ins.add(new FieldInsnNode(PUTSTATIC, "com/alpha/AlphaTransformer", "logged", "Z"));
                ins.add(skip);
                m.instructions.insert(ins);
                touched = true;
            }
            if (!touched) {
                return basicClass;
            }
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            return basicClass;
        }
    }
}
