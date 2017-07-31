package ro.pub.cs.diploma;

import com.intellij.codeInsight.daemon.impl.RecursiveCallLineMarkerProvider;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.siyeh.ig.performance.TailRecursionInspection;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class RecursionUtil {
  private RecursionUtil() {

  }

  /**
   * Checks if the {@code expression} is a recursive method call to {@code method}.
   *
   * @see RecursiveCallLineMarkerProvider#isRecursiveMethodCall(PsiMethodCallExpression)
   * @see TailRecursionInspection.TailRecursionVisitor#visitReturnStatement(PsiReturnStatement)
   * @see com.siyeh.ig.psiutils.RecursionUtils
   * @see com.siyeh.ig.psiutils.RecursionVisitor
   */
  static boolean isRecursive(@NotNull final PsiMethodCallExpression expression, @NotNull final PsiMethod method) {
    final PsiReferenceExpression methodExpression = expression.getMethodExpression();
    if (!method.getName().equals(methodExpression.getReferenceName())) {
      return false;
    }
    final PsiMethod calledMethod = expression.resolveMethod();
    if (!method.equals(calledMethod)) {
      return false;
    }
    if (method.hasModifierProperty(PsiModifier.STATIC) || method.hasModifierProperty(PsiModifier.PRIVATE)) {
      return true;
    }
    final PsiExpression qualifier = ParenthesesUtils.stripParentheses(methodExpression.getQualifierExpression());
    return qualifier == null || qualifier instanceof PsiThisExpression;
  }

  /**
   * Returns true if the specified {@code element} contains at least on recursive call to the specified {@code method}.
   */
  static boolean containsRecursiveCalls(@NotNull final PsiElement element, @NotNull final PsiMethod method) {
    final Ref<Boolean> contains = new Ref<>(false);
    element.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (!isRecursive(expression, method)) {
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

  static boolean hasToBeSavedOnStack(@NotNull final PsiParameter parameter, @NotNull final PsiMethod method) {
    final PsiElement parent = parameter.getParent();
    if (!(parent instanceof PsiForeachStatement)) {
      return true;
    }
    final PsiStatement body = ((PsiLoopStatement)parent).getBody();
    return body != null && containsRecursiveCalls(body, method);
  }

  static boolean hasToBeSavedOnStack(@NotNull final PsiDeclarationStatement statement, @NotNull final PsiMethod method) {
    final PsiElement parent = statement.getParent();
    if (parent instanceof PsiForStatement && !containsRecursiveCalls(parent, method)) {
      return false;
    }
    if (parent instanceof PsiCodeBlock) {
      PsiCodeBlock block = (PsiCodeBlock)parent;
      List<PsiStatement> statements = new ArrayList<>();
      boolean met = false;
      for (PsiStatement psiStatement : block.getStatements()) {
        if (met) {
          statements.add(psiStatement);
        }
        if (psiStatement == statement) {
          met = true;
        }
      }
      for (PsiStatement psiStatement : statements) {
        if (containsRecursiveCalls(psiStatement, method)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }
}
