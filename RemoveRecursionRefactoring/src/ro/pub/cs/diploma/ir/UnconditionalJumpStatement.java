package ro.pub.cs.diploma.ir;

import com.intellij.openapi.util.Ref;

public class UnconditionalJumpStatement implements TerminatorStatement {
  private final Ref<Block> blockRef;

  public UnconditionalJumpStatement(Ref<Block> blockRef) {
    this.blockRef = blockRef;
  }

  public Block getBlock() {
    return blockRef.get();
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
