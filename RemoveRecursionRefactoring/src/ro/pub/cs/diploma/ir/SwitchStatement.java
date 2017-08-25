package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiExpression;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SwitchStatement implements TerminatorStatement {
  @NotNull private final PsiExpression myExpression;
  @NotNull private final List<Statement> myStatements;

  SwitchStatement(@NotNull final PsiExpression expression,
                  @NotNull final List<Statement> statements) {
    myExpression = expression;
    myStatements = statements;
  }

  @NotNull
  public PsiExpression getExpression() {
    return myExpression;
  }

  @NotNull
  public List<Statement> getStatements() {
    return myStatements;
  }

  @Override
  public void accept(@NotNull Visitor visitor) {
    visitor.visit(this);
  }
}
