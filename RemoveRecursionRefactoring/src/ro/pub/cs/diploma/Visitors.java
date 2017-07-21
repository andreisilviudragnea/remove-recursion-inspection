package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.List;

class Visitors {
  static List<PsiMethodCallExpression> extractRecursiveCalls(PsiCodeBlock block) {
    final List<PsiMethodCallExpression> calls = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        final PsiMethod containingMethod = IterativeMethodGenerator.isRecursiveMethodCall(expression);
        if (containingMethod == null) {
          return;
        }
        calls.add(expression);
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
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

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
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

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });
    return returnStatements;
  }

  static boolean containsRecursiveCalls(PsiStatement statement) {
    final Ref<Boolean> contains = new Ref<>(false);
    statement.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        final PsiMethod containingMethod = IterativeMethodGenerator.isRecursiveMethodCall(expression);
        if (containingMethod == null) {
          return;
        }
        contains.set(true);
        stopWalking();
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });
    return contains.get();
  }

  static void replaceSingleStatementsWithBlockStatements(PsiElementFactory factory, PsiMethod method) {
    method.accept(new JavaRecursiveElementWalkingVisitor() {
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

  static void replaceForEachStatementsWithForStatements(PsiMethod method) {
    final List<PsiForeachStatement> foreachStatements = new ArrayList<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitForeachStatement(PsiForeachStatement statement) {
        super.visitForeachStatement(statement);
        foreachStatements.add(statement);
      }
    });
    for (PsiForeachStatement foreachStatement : foreachStatements) {
      Refactorings.replaceForEachStatementWithIteratorForLoopStatement(foreachStatement, method);
    }
  }

  static void replaceForStatementsWithWhileStatements(PsiMethod method) {
    final List<PsiForStatement> forStatements = new ArrayList<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitForStatement(PsiForStatement statement) {
        super.visitForStatement(statement);
        forStatements.add(statement);
      }
    });
    forStatements.forEach(Refactorings::replaceForStatementWithWhileStatement);
  }
}
