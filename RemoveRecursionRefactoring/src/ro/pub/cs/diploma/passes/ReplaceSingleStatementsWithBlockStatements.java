package ro.pub.cs.diploma.passes;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReplaceSingleStatementsWithBlockStatements implements Pass<PsiMethod, List<PsiStatement>, Object> {
  @NotNull private final PsiElementFactory myFactory;

  private ReplaceSingleStatementsWithBlockStatements(@NotNull final PsiElementFactory factory) {
    myFactory = factory;
  }

  @NotNull
  public static ReplaceSingleStatementsWithBlockStatements getInstance(@NotNull final PsiElementFactory factory) {
    return new ReplaceSingleStatementsWithBlockStatements(factory);
  }

  @Override
  public List<PsiStatement> collect(PsiMethod method) {
    final List<PsiStatement> statements = new ArrayList<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitIfStatement(PsiIfStatement statement) {
        super.visitIfStatement(statement);
        statements.add(statement.getThenBranch());
        final PsiStatement elseBranch = statement.getElseBranch();
        if (elseBranch != null) {
          statements.add(elseBranch);
        }
      }

      @Override
      public void visitForStatement(PsiForStatement statement) {
        super.visitForStatement(statement);
        statements.add(statement.getBody());
      }

      @Override
      public void visitWhileStatement(PsiWhileStatement statement) {
        super.visitWhileStatement(statement);
        statements.add(statement.getBody());
      }

      @Override
      public void visitDoWhileStatement(PsiDoWhileStatement statement) {
        super.visitDoWhileStatement(statement);
        statements.add(statement.getBody());
      }

      @Override
      public void visitForeachStatement(PsiForeachStatement statement) {
        super.visitForeachStatement(statement);
        statements.add(statement.getBody());
      }
    });
    return statements;
  }

  @Override
  public Object transform(List<PsiStatement> statements) {
    statements.forEach(statement -> {
      if (statement instanceof PsiEmptyStatement) {
        statement.replace(myFactory.createExpressionFromText("{}", null));
      }
      else if (!(statement instanceof PsiBlockStatement)) {
        statement.replace(myFactory.createStatementFromText("{" + statement.getText() + "}", null));
      }
    });
    return null;
  }
}
