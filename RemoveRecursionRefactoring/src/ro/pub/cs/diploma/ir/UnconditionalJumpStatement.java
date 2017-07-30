package ro.pub.cs.diploma.ir;

import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;

public class UnconditionalJumpStatement implements TerminatorStatement {
  @NotNull private final Ref<Block> blockRef;

  public UnconditionalJumpStatement(@NotNull Ref<Block> blockRef) {
    this.blockRef = blockRef;
  }

  @NotNull
  public Block getBlock() {
    return blockRef.get();
  }

  @Override
  public void accept(@NotNull final Visitor visitor) {
    visitor.visit(this);
  }
}
