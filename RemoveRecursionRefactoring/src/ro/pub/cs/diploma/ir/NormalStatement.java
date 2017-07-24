package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiStatement;

public class NormalStatement implements Statement {
  private final PsiStatement statement;

  public NormalStatement(PsiStatement statement) {
    this.statement = statement;
  }

  public PsiStatement getStatement() {
    return statement;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
