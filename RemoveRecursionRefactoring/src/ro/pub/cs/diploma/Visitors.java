package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class Visitors {
  static List<PsiMethodCallExpression> extractRecursiveCalls(@NotNull final PsiMethod method) {
    final List<PsiMethodCallExpression> calls = new ArrayList<>();
    method.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (Util.isRecursive(expression, method)) {
          calls.add(expression);
        }
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

  static boolean containsRecursiveCalls(@NotNull final PsiElement element, @NotNull final PsiMethod method) {
    final Ref<Boolean> contains = new Ref<>(false);
    element.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (!Util.isRecursive(expression, method)) {
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

  static void replaceSingleStatementsWithBlockStatements(PsiMethod method) {
    final PsiElementFactory factory = Util.getFactory(method);
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

  static void replaceForEachLoopsWithIteratorForLoops(PsiMethod method) {
    final List<PsiForeachStatement> foreachStatements = new ArrayList<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitForeachStatement(PsiForeachStatement statement) {
        super.visitForeachStatement(statement);
        foreachStatements.add(statement);
      }
    });
    for (PsiForeachStatement foreachStatement : foreachStatements) {
      if (containsRecursiveCalls(foreachStatement, method)) {
        Refactorings.replaceForEachLoopWithIteratorForLoop(foreachStatement, method);
      }
    }
  }

  static void replaceForEachLoopsWithIndexedForLoops(PsiMethod method) {
    final List<PsiForeachStatement> foreachStatements = new ArrayList<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitForeachStatement(PsiForeachStatement statement) {
        super.visitForeachStatement(statement);
        foreachStatements.add(statement);
      }
    });
    for (PsiForeachStatement foreachStatement : foreachStatements) {
      if (containsRecursiveCalls(foreachStatement, method)) {
        Refactorings.replaceForEachLoopWithIndexedForLoop(foreachStatement, method);
      }
    }
  }
}
