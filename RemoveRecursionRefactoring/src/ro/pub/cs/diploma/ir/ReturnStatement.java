package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiReturnStatement;
import org.jetbrains.annotations.NotNull;

public class ReturnStatement implements TerminatorStatement {
  @NotNull private final PsiReturnStatement statement;

  public ReturnStatement(@NotNull final PsiReturnStatement statement) {
    this.statement = statement;
  }

  @NotNull
  public PsiReturnStatement getStatement() {
    return statement;
  }

  @Override
  public void accept(@NotNull final Visitor visitor) {
    visitor.visit(this);
  }
}
