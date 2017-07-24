package ro.pub.cs.diploma.ir;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiExpression;

public class ConditionalJumpStatement implements TerminatorStatement {
  private final PsiExpression condition;
  private final Ref<Block> thenBlockRef;
  private final Ref<Block> elseBlockRef;

  public ConditionalJumpStatement(PsiExpression condition, Ref<Block> thenBlockRef, Ref<Block> elseBlockRef) {
    this.condition = condition;
    this.thenBlockRef = thenBlockRef;
    this.elseBlockRef = elseBlockRef;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }

  public PsiExpression getCondition() {
    return condition;
  }

  public Block getThenBlock() {
    return thenBlockRef.get();
  }

  public Block getElseBlock() {
    return elseBlockRef.get();
  }
}
