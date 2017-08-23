package ro.pub.cs.diploma.passes;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ro.pub.cs.diploma.Util;

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

      @Override
      public void visitSwitchStatement(PsiSwitchStatement statement) {
        super.visitSwitchStatement(statement);
        final PsiCodeBlock block = statement.getBody();
        if (block == null) {
          return;
        }
        final PsiElementFactory factory = Util.getFactory(statement);
        PsiCodeBlock currentCodeBlock = null;
        PsiSwitchLabelStatement currentLabelStatement = null;
        for (final PsiStatement psiStatement : block.getStatements()) {
          if (psiStatement instanceof PsiSwitchLabelStatement) {
            addBlock(block, factory, currentLabelStatement, currentCodeBlock);
            currentLabelStatement = (PsiSwitchLabelStatement)psiStatement;
            currentCodeBlock = factory.createCodeBlock();
            continue;
          }
          if (currentCodeBlock != null && !(psiStatement instanceof PsiEmptyStatement)) {
            currentCodeBlock.add(psiStatement);
          }
          psiStatement.delete();
        }
        addBlock(block, factory, currentLabelStatement, currentCodeBlock);
      }

      private void addBlock(@NotNull final PsiCodeBlock block,
                            @NotNull final PsiElementFactory factory,
                            @Nullable final PsiSwitchLabelStatement currentLabelStatement,
                            @Nullable final PsiCodeBlock currentCodeBlock) {
        if (currentLabelStatement == null || currentCodeBlock == null || currentCodeBlock.getStatements().length == 0) {
          return;
        }
        final PsiStatement[] currentCodeBlockStatements = currentCodeBlock.getStatements();
        final PsiStatement statementToAdd;
        if (currentCodeBlockStatements.length == 1 && currentCodeBlockStatements[0] instanceof PsiBlockStatement) {
          statementToAdd = currentCodeBlockStatements[0];
        }
        else {
          statementToAdd = factory.createStatementFromText(currentCodeBlock.getText(), null);
        }
        block.addAfter(statementToAdd, currentLabelStatement);
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
