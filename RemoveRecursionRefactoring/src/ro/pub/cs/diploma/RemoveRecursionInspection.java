package ro.pub.cs.diploma;

import com.intellij.codeInsight.daemon.impl.RecursiveCallLineMarkerProvider;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.performance.TailRecursionInspection;
import com.siyeh.ig.psiutils.MethodUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @see TailRecursionInspection
 */
public class RemoveRecursionInspection extends BaseInspection {

  @Override
  @NotNull
  public String getDisplayName() {
    return RemoveRecursionBundle.message("remove.recursion.display.name");
  }

  @Override
  @NotNull
  protected String buildErrorString(Object... infos) {
    return RemoveRecursionBundle.message("remove.recursion.problem.descriptor");
  }

  @Override
  @Nullable
  protected InspectionGadgetsFix buildFix(Object... infos) {
    final PsiMethod containingMethod = (PsiMethod)infos[0];
    if (!mayBeReplacedByIterativeMethod(containingMethod)) {
      return null;
    }
    return new RemoveTailRecursionFix();
  }

  private static boolean mayBeReplacedByIterativeMethod(PsiMethod containingMethod) {
    if (containingMethod.isVarArgs()) {
      return false;
    }
    final PsiParameter[] parameters = containingMethod.getParameterList().getParameters();
    for (final PsiParameter parameter : parameters) {
      if (parameter.hasModifierProperty(PsiModifier.FINAL)) {
        return false;
      }
    }
    return true;
  }

  private static class RemoveTailRecursionFix extends InspectionGadgetsFix {

    @Override
    @NotNull
    public String getFamilyName() {
      return RemoveRecursionBundle.message("remove.recursion.replace.quickfix");
    }

    @Override
    public void doFix(Project project, ProblemDescriptor descriptor) {
      final PsiElement callToken = descriptor.getPsiElement();
      final PsiMethod method =
        PsiTreeUtil.getParentOfType(callToken, PsiMethod.class, true, PsiClass.class, PsiLambdaExpression.class);
      if (method == null) {
        return;
      }
      RemoveRecursionRefactoringAction.removeRecursion(method, project, true);
    }
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new RemoveRecursionVisitor();
  }

  private static class RemoveRecursionVisitor extends BaseInspectionVisitor {
    /**
     * @see RecursiveCallLineMarkerProvider#isRecursiveMethodCall(PsiMethodCallExpression)
     * @see TailRecursionInspection.TailRecursionVisitor#visitReturnStatement(PsiReturnStatement)
     */
    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      final PsiReferenceExpression methodExpression = expression.getMethodExpression();
      final PsiMethod containingMethod =
        PsiTreeUtil.getParentOfType(expression, PsiMethod.class, true, PsiClass.class, PsiLambdaExpression.class);
      if (containingMethod == null) {
        return;
      }
      final JavaResolveResult resolveResult = expression.resolveMethodGenerics();
      if (!resolveResult.isValidResult() || !containingMethod.equals(resolveResult.getElement())) {
        return;
      }
      final PsiExpression qualifier = ParenthesesUtils.stripParentheses(methodExpression.getQualifierExpression());
      if (qualifier != null && !(qualifier instanceof PsiThisExpression) && MethodUtils.isOverridden(containingMethod)) {
        return;
      }
      registerMethodCallError(expression, containingMethod);
    }
  }
}