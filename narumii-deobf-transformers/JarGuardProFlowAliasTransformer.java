package uwu.narumi.deobfuscator.core.other.impl.jarguardpro;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves flattened-flow trampoline labels: label blocks containing only GOTO.
 */
public class JarGuardProFlowAliasTransformer extends Transformer {
  @Override
  protected void transform() {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(this::processMethod));
  }

  private void processMethod(MethodNode methodNode) {
    Map<LabelNode, LabelNode> directAliases = new HashMap<>();

    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (!(insn instanceof LabelNode label)) continue;
      AbstractInsnNode next = nextRealInsn(label);
      if (next instanceof JumpInsnNode jumpInsn && jumpInsn.getOpcode() == GOTO) {
        directAliases.put(label, jumpInsn.label);
      }
    }

    if (directAliases.isEmpty()) return;

    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn instanceof JumpInsnNode jumpInsn) {
        LabelNode resolved = resolve(jumpInsn.label, directAliases);
        if (resolved != jumpInsn.label) {
          jumpInsn.label = resolved;
          markChange();
        }
      } else if (insn instanceof LookupSwitchInsnNode lookup) {
        LabelNode newDflt = resolve(lookup.dflt, directAliases);
        if (newDflt != lookup.dflt) {
          lookup.dflt = newDflt;
          markChange();
        }
        for (int i = 0; i < lookup.labels.size(); i++) {
          LabelNode old = lookup.labels.get(i);
          LabelNode resolved = resolve(old, directAliases);
          if (resolved != old) {
            lookup.labels.set(i, resolved);
            markChange();
          }
        }
      } else if (insn instanceof TableSwitchInsnNode table) {
        LabelNode newDflt = resolve(table.dflt, directAliases);
        if (newDflt != table.dflt) {
          table.dflt = newDflt;
          markChange();
        }
        for (int i = 0; i < table.labels.size(); i++) {
          LabelNode old = table.labels.get(i);
          LabelNode resolved = resolve(old, directAliases);
          if (resolved != old) {
            table.labels.set(i, resolved);
            markChange();
          }
        }
      }
    }
  }

  private static LabelNode resolve(LabelNode start, Map<LabelNode, LabelNode> aliases) {
    LabelNode current = start;
    Set<LabelNode> seen = new HashSet<>();
    while (aliases.containsKey(current) && seen.add(current)) {
      current = aliases.get(current);
    }
    return current;
  }

  private static AbstractInsnNode nextRealInsn(AbstractInsnNode insn) {
    AbstractInsnNode next = insn.getNext();
    while (next != null && (next.getType() == AbstractInsnNode.LABEL || next.getType() == AbstractInsnNode.LINE || next.getType() == AbstractInsnNode.FRAME)) {
      next = next.getNext();
    }
    return next;
  }
}
