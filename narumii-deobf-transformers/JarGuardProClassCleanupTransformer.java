package uwu.narumi.deobfuscator.core.other.impl.jarguardpro;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

/**
 * Removes trivial helper classes left after inlining (no fields, only default ctor).
 */
public class JarGuardProClassCleanupTransformer extends Transformer {
  @Override
  protected void transform() {
    Set<String> removeClasses = new HashSet<>();

    scopedClasses().forEach(classWrapper -> {
      if (!classWrapper.fields().isEmpty()) return;
      if (classWrapper.methods().size() != 1) return;

      MethodNode init = classWrapper.methods().get(0);
      if (!"<init>".equals(init.name) || !"()V".equals(init.desc)) return;

      AbstractInsnNode[] insns = init.instructions.toArray();
      int realCount = 0;
      for (AbstractInsnNode insn : insns) {
        if (insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.FRAME) {
          continue;
        }
        realCount++;
      }
      // Expected bytecode: ALOAD_0, INVOKESPECIAL Object.<init>(), RETURN
      if (realCount <= 4) {
        removeClasses.add(classWrapper.name());
      }
    });

    removeClasses.forEach(name -> {
      context().getClassesMap().remove(name);
      markChange();
    });
  }
}
