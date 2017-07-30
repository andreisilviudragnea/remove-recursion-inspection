package ro.pub.cs.diploma.ir;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiExpression;
import org.jetbrains.annotations.NotNull;

public class ConditionalJumpStatement implements TerminatorStatement {
  @NotNull private final PsiExpression condition;
  @NotNull private final Ref<Block> thenBlockRef;
  @NotNull private final Ref<Block> elseBlockRef;

  public ConditionalJumpStatement(@NotNull final PsiExpression condition,
                                  @NotNull final Ref<Block> thenBlockRef,
                                  @NotNull final Ref<Block> elseBlockRef) {
    this.condition = condition;
    this.thenBlockRef = thenBlockRef;
    this.elseBlockRef = elseBlockRef;
  }

  @Override
  public void accept(@NotNull final Visitor visitor) {
    visitor.visit(this);
  }

  @NotNull
  PsiExpression getCondition() {
    return condition;
  }

  @NotNull
  Block getThenBlock() {
    return thenBlockRef.get();
  }

  @NotNull
  Block getElseBlock() {
    return elseBlockRef.get();
  }
}
