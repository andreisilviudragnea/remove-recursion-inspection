package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiReturnStatement;

public class ReturnStatement implements TerminatorStatement {
  private final PsiReturnStatement statement;

  public ReturnStatement(PsiReturnStatement statement) {
    this.statement = statement;
  }

  public PsiReturnStatement getStatement() {
    return statement;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
