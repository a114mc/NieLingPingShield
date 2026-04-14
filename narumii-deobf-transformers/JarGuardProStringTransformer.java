package uwu.narumi.deobfuscator.core.other.impl.jarguardpro;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inlines `new String(helper()[B])` where helper methods return constant byte arrays.
 */
public class JarGuardProStringTransformer extends Transformer {
  private record MethodKey(String owner, String name, String desc) {}

  @Override
  protected void transform() {
    Map<MethodKey, String> decodedStrings = new HashMap<>();
    Set<MethodKey> helperMethods = new HashSet<>();
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      parseStaticByteArrayFactory(methodNode).ifPresent(bytes -> {
        MethodKey key = new MethodKey(classWrapper.name(), methodNode.name, methodNode.desc);
        decodedStrings.put(key, new String(bytes));
        helperMethods.add(key);
      });
    }));

    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (!(insn instanceof TypeInsnNode typeInsn) || typeInsn.getOpcode() != NEW || !"java/lang/String".equals(typeInsn.desc)) continue;
        AbstractInsnNode dup = nextRealInsn(insn);
        AbstractInsnNode call = nextRealInsn(dup);
        AbstractInsnNode init = nextRealInsn(call);

        if (dup == null || dup.getOpcode() != DUP) continue;
        if (!(call instanceof MethodInsnNode staticCall) || staticCall.getOpcode() != INVOKESTATIC || !"()[B".equals(staticCall.desc)) continue;
        if (!(init instanceof MethodInsnNode initCall)
            || initCall.getOpcode() != INVOKESPECIAL
            || !"java/lang/String".equals(initCall.owner)
            || !"<init>".equals(initCall.name)
            || !"([B)V".equals(initCall.desc)) continue;

        String decoded = decodedStrings.get(new MethodKey(staticCall.owner, staticCall.name, staticCall.desc));
        if (decoded == null) continue;

        methodNode.instructions.set(insn, new LdcInsnNode(decoded));
        methodNode.instructions.remove(dup);
        methodNode.instructions.remove(call);
        methodNode.instructions.remove(init);
        markChange();
      }
    }));

    cleanupHelperClasses(helperMethods);
  }

  private static java.util.Optional<byte[]> parseStaticByteArrayFactory(MethodNode methodNode) {
    if ((methodNode.access & ACC_STATIC) == 0) return java.util.Optional.empty();
    if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) return java.util.Optional.empty();
    if (Type.getArgumentTypes(methodNode.desc).length != 0) return java.util.Optional.empty();
    if (!Type.getReturnType(methodNode.desc).equals(Type.getType(byte[].class))) return java.util.Optional.empty();

    List<AbstractInsnNode> ops = compact(methodNode);
    if (ops.size() < 3) return java.util.Optional.empty();
    if (!ops.get(0).isConstant()) return java.util.Optional.empty();
    if (!(ops.get(1) instanceof IntInsnNode newArray) || newArray.getOpcode() != NEWARRAY || newArray.operand != T_BYTE) {
      return java.util.Optional.empty();
    }
    if (ops.get(ops.size() - 1).getOpcode() != ARETURN) return java.util.Optional.empty();

    Integer size = toInt(ops.get(0));
    if (size == null || size < 0) return java.util.Optional.empty();

    byte[] bytes = new byte[size];
    int i = 2;
    while (i < ops.size() - 1) {
      if (i + 3 >= ops.size()) return java.util.Optional.empty();
      if (ops.get(i).getOpcode() != DUP) return java.util.Optional.empty();

      Integer index = toInt(ops.get(i + 1));
      Integer value = toInt(ops.get(i + 2));
      if (index == null || value == null) return java.util.Optional.empty();
      if (ops.get(i + 3).getOpcode() != BASTORE) return java.util.Optional.empty();
      if (index < 0 || index >= bytes.length) return java.util.Optional.empty();

      bytes[index] = (byte) (value & 0xFF);
      i += 4;
    }

    if (i != ops.size() - 1) return java.util.Optional.empty();
    return java.util.Optional.of(bytes);
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

  private static Integer toInt(AbstractInsnNode insn) {
    if (!insn.isConstant()) return null;
    Object cst = insn.asConstant();
    if (!(cst instanceof Number number)) return null;
    return number.intValue();
  }

  private static AbstractInsnNode nextRealInsn(AbstractInsnNode insn) {
    if (insn == null) return null;
    AbstractInsnNode next = insn.getNext();
    while (next != null && (next.getType() == AbstractInsnNode.LABEL || next.getType() == AbstractInsnNode.LINE || next.getType() == AbstractInsnNode.FRAME)) {
      next = next.getNext();
    }
    return next;
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
