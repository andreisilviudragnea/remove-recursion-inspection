package ro.pub.cs.diploma;

import com.intellij.codeInsight.daemon.impl.RecursiveCallLineMarkerProvider;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.performance.TailRecursionInspection;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class Util {
  private Util() {
  }

  static boolean hasToBeSavedOnStack(@NotNull final PsiParameter parameter, @NotNull final PsiMethod method) {
    final PsiElement parent = parameter.getParent();
    return !(parent instanceof PsiForeachStatement) || Visitors.containsRecursiveCalls(parent, method);
  }

  static boolean hasToBeSavedOnStack(@NotNull final PsiDeclarationStatement statement, @NotNull final PsiMethod method) {
    final PsiElement parent = statement.getParent();
    if (parent instanceof PsiForStatement && !Visitors.containsRecursiveCalls(parent, method)) {
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
        if (Visitors.containsRecursiveCalls(psiStatement, method)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  @NotNull
  static PsiStatement statement(@NotNull final PsiElementFactory factory, @NotNull final String text) {
    return factory.createStatementFromText(text, null);
  }

  @NotNull
  static PsiElementFactory getFactory(@NotNull final PsiElement element) {
    return JavaPsiFacade.getElementFactory(element.getProject());
  }

  @NotNull
  static JavaCodeStyleManager getStyleManager(@NotNull final PsiElement element) {
    return JavaCodeStyleManager.getInstance(element.getProject());
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
