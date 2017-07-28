package ro.pub.cs.diploma;

import com.intellij.codeInsight.daemon.impl.RecursiveCallLineMarkerProvider;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.performance.TailRecursionInspection;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Util {
  @NotNull
  static PsiElementFactory getFactory(@NotNull final PsiElement element) {
    return JavaPsiFacade.getElementFactory(element.getProject());
  }

  @Nullable
  static PsiMethod getContainingMethod(@NotNull final PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, PsiMethod.class, true, PsiClass.class, PsiLambdaExpression.class);
  }

  @NotNull
  @Contract(pure = true)
  static String getFrameClassName(@NotNull final String methodName) {
    return Utilities.capitalize(methodName) + Constants.FRAME;
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
}
