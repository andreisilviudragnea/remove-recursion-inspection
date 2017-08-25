package ro.pub.cs.diploma.ir;

import org.jetbrains.annotations.NotNull;

public interface Visitor {
  void visit(@NotNull final Block block);

  void visit(@NotNull final ConditionalJumpStatement conditionalJumpStatement);

  void visit(@NotNull final NormalStatement normalStatement);

  void visit(@NotNull final ReturnStatement returnStatement);

  void visit(@NotNull final UnconditionalJumpStatement unconditionalJumpStatement);

  void visit(@NotNull final SwitchStatement switchStatement);
}
