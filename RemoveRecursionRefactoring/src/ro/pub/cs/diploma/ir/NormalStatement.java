package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiStatement;
import org.jetbrains.annotations.NotNull;

public class NormalStatement implements Statement, WrapperStatement {
  @NotNull private final PsiStatement statement;

  NormalStatement(@NotNull final PsiStatement statement) {
    this.statement = statement;
  }

  @NotNull
  public PsiStatement getStatement() {
    return statement;
  }

  @Override
  public void accept(@NotNull final Visitor visitor) {
    visitor.visit(this);
  }
}
