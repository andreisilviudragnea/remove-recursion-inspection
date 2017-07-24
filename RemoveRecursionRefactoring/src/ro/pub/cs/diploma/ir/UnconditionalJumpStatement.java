package ro.pub.cs.diploma.ir;

public class UnconditionalJumpStatement implements TerminatorStatement {
  private final Block block;

  public UnconditionalJumpStatement(Block block) {
    this.block = block;
  }

  public Block getBlock() {
    return block;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
