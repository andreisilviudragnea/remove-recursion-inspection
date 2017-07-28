package ro.pub.cs.diploma;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.performance.TailRecursionInspection;
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
      final PsiMethod method = Util.getContainingMethod(callToken);
      if (method == null) {
        return;
      }
      IterativeMethodGenerator.createIterativeBody(method, project, true);
    }
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new RemoveRecursionVisitor();
  }

  private static class RemoveRecursionVisitor extends BaseInspectionVisitor {
    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      final PsiMethod method = Util.getContainingMethod(expression);
      if (method == null) {
        return;
      }
      if (Util.isRecursive(expression, method)) {
        registerMethodCallError(expression, method);
      }
    }
  }
}