package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiExpression;

public class ConditionalJumpStatement implements TerminatorStatement {
  private final PsiExpression condition;
  private final Block thenBlock;
  private final Block elseBlock;

  public ConditionalJumpStatement(PsiExpression condition, Block thenBlock, Block elseBlock) {
    this.condition = condition;
    this.thenBlock = thenBlock;
    this.elseBlock = elseBlock;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }

  public PsiExpression getCondition() {
    return condition;
  }

  public Block getThenBlock() {
    return thenBlock;
  }

  public Block getElseBlock() {
    return elseBlock;
  }
}
