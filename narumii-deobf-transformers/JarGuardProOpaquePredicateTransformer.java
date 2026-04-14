package uwu.narumi.deobfuscator.core.other.impl.jarguardpro;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Inlines trivial static helper calls used as JarGuardPro opaque predicates.
 */
public class JarGuardProOpaquePredicateTransformer extends Transformer {
  private record MethodKey(String owner, String name, String desc) {}

  @Override
  protected void transform() {
    Set<MethodKey> helperMethods = new HashSet<>();
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      Optional<Object> constant = evaluateNoArgConstantMethod(methodNode);
      if (constant.isPresent()) {
        helperMethods.add(new MethodKey(classWrapper.name(), methodNode.name, methodNode.desc));
      }
    }));

    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (!(insn instanceof MethodInsnNode methodInsn)) continue;
        if (methodInsn.getOpcode() != INVOKESTATIC) continue;
        if (Type.getArgumentTypes(methodInsn.desc).length != 0) continue;

        ClassWrapper ownerClass = context().getClassesMap().get(methodInsn.owner);
        if (ownerClass == null) continue;

        Optional<MethodNode> maybeTarget = ownerClass.findMethod(methodInsn.name, methodInsn.desc);
        if (maybeTarget.isEmpty()) continue;

        Optional<Object> constant = evaluateNoArgConstantMethod(maybeTarget.get());
        if (constant.isEmpty()) continue;

        methodNode.instructions.set(insn, AsmHelper.toConstantInsn(constant.get()));
        markChange();
      }
    }));

    cleanupHelperClasses(helperMethods);
  }

  private Optional<Object> evaluateNoArgConstantMethod(MethodNode methodNode) {
    if ((methodNode.access & ACC_STATIC) == 0) return Optional.empty();
    if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) return Optional.empty();
    if (Type.getArgumentTypes(methodNode.desc).length != 0) return Optional.empty();

    Type returnType = Type.getReturnType(methodNode.desc);
    if (returnType.getSort() == Type.VOID || returnType.getSort() == Type.ARRAY || returnType.getSort() == Type.OBJECT) {
      return Optional.empty();
    }

    List<AbstractInsnNode> ops = compact(methodNode);

    // Pattern: <const>; IRETURN/LRETURN/... (for primitive types)
    if (ops.size() == 2 && ops.get(0).isConstant() && isPrimitiveReturn(ops.get(1), returnType)) {
      return Optional.of(ops.get(0).asConstant());
    }

    // Pattern: LDC "text"; INVOKEVIRTUAL java/lang/String.hashCode()I; IRETURN
    if (returnType.equals(Type.INT_TYPE) && ops.size() == 3 && ops.get(0) instanceof LdcInsnNode ldc
        && ldc.cst instanceof String text
        && ops.get(1) instanceof MethodInsnNode call
        && call.getOpcode() == INVOKEVIRTUAL
        && call.owner.equals("java/lang/String")
        && call.name.equals("hashCode")
        && call.desc.equals("()I")
        && ops.get(2).getOpcode() == IRETURN) {
      return Optional.of(text.hashCode());
    }

    return Optional.empty();
  }

  private static List<AbstractInsnNode> compact(MethodNode methodNode) {
    List<AbstractInsnNode> list = new ArrayList<>();
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.FRAME) {
        continue;
      }
      list.add(insn);
    }
    return list;
  }

  private static boolean isPrimitiveReturn(AbstractInsnNode insn, Type returnType) {
    return switch (returnType.getSort()) {
      case Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.CHAR, Type.INT -> insn.getOpcode() == IRETURN;
      case Type.LONG -> insn.getOpcode() == LRETURN;
      case Type.FLOAT -> insn.getOpcode() == FRETURN;
      case Type.DOUBLE -> insn.getOpcode() == DRETURN;
      default -> false;
    };
  }

  private void cleanupHelperClasses(Set<MethodKey> helperMethods) {
    Set<MethodKey> usedMethods = new HashSet<>();
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn instanceof MethodInsnNode methodInsn && methodInsn.getOpcode() == INVOKESTATIC) {
          usedMethods.add(new MethodKey(methodInsn.owner, methodInsn.name, methodInsn.desc));
        }
      }
    }));

    Set<String> removeClasses = new HashSet<>();
    scopedClasses().forEach(classWrapper -> {
      if (!classWrapper.fields().isEmpty()) return;
      if (classWrapper.methods().size() != 2) return;

      MethodNode helperMethod = classWrapper.methods().stream()
          .filter(methodNode -> !methodNode.name.equals("<init>"))
          .findFirst()
          .orElse(null);
      if (helperMethod == null) return;

      MethodKey key = new MethodKey(classWrapper.name(), helperMethod.name, helperMethod.desc);
      if (!helperMethods.contains(key)) return;
      if (usedMethods.contains(key)) return;

      removeClasses.add(classWrapper.name());
    });

    removeClasses.forEach(name -> {
      context().getClassesMap().remove(name);
      markChange();
    });
  }
}
