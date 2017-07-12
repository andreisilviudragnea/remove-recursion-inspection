package ro.pub.cs.diploma;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Visitors {
  static List<Variable> extractVariables(PsiMethod method) {
    return Arrays
      .stream(method.getParameterList().getParameters())
      .map(parameter -> new Variable(parameter.getName(), parameter.getType().getPresentableText()))
      .collect(Collectors.toList());
  }

  // TODO: Replace the name check with something that checks the resolved reference.
  static List<PsiMethodCallExpression> extractRecursiveCalls(PsiCodeBlock block, String name) {
    final List<PsiMethodCallExpression> calls = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (expression.getMethodExpression().getReferenceName().equals(name)) {
          calls.add(expression);
        }
      }
    });
    return calls;
  }

  static List<PsiDeclarationStatement> extractDeclarationStatements(PsiCodeBlock block) {
    final List<PsiDeclarationStatement> declarations = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitDeclarationStatement(PsiDeclarationStatement statement) {
        super.visitDeclarationStatement(statement);
        declarations.add(statement);
      }
    });
    return declarations;
  }

  static List<PsiReturnStatement> extractReturnStatements(PsiCodeBlock block) {
    final List<PsiReturnStatement> returnStatements = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitReturnStatement(PsiReturnStatement statement) {
        super.visitReturnStatement(statement);
        returnStatements.add(statement);
      }
    });
    return returnStatements;
  }

  static List<PsiReferenceExpression> extractReferenceExpressions(PsiCodeBlock block) {
    final List<PsiReferenceExpression> expressions = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitReferenceExpression(PsiReferenceExpression expression) {
        super.visitReferenceExpression(expression);
        expressions.add(expression);
      }
    });
    return expressions;
  }

  static void replaceSingleStatementsWithBlockStatements(PsiElementFactory factory, PsiCodeBlock block) {
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      private void replaceStatement(PsiStatement statement) {
        if (!(statement instanceof PsiBlockStatement)) {
          statement.replace(factory.createStatementFromText("{" + statement.getText() + "}", null));
        }
      }

      @Override
      public void visitIfStatement(PsiIfStatement statement) {
        super.visitIfStatement(statement);
        replaceStatement(statement.getThenBranch());
        final PsiStatement elseBranch = statement.getElseBranch();
        if (elseBranch != null) {
          replaceStatement(elseBranch);
        }
      }

      @Override
      public void visitForStatement(PsiForStatement statement) {
        super.visitForStatement(statement);
        replaceStatement(statement.getBody());
      }

      @Override
      public void visitWhileStatement(PsiWhileStatement statement) {
        super.visitWhileStatement(statement);
        replaceStatement(statement.getBody());
      }

      @Override
      public void visitDoWhileStatement(PsiDoWhileStatement statement) {
        super.visitDoWhileStatement(statement);
        replaceStatement(statement.getBody());
      }

      @Override
      public void visitForeachStatement(PsiForeachStatement statement) {
        super.visitForeachStatement(statement);
        replaceStatement(statement.getBody());
      }
    });
  }

  static void replaceForEachStatementsWithForStatements(PsiCodeBlock block) {
    block.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitForeachStatement(PsiForeachStatement statement) {
        super.visitForeachStatement(statement);
        Refactorings.replaceForEachStatementWithIteratorForLoopStatement(statement);
      }
    });
  }

  static void replaceForStatementsWithWhileStatements(PsiCodeBlock block) {
    block.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitForStatement(PsiForStatement statement) {
        super.visitForStatement(statement);
        Refactorings.replaceForStatementWithWhileStatement(statement);
      }
    });
  }
}
