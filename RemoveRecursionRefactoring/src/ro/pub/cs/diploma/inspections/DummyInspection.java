package ro.pub.cs.diploma.inspections;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ro.pub.cs.diploma.IterativeMethodGenerator;
import ro.pub.cs.diploma.NameManager;
import ro.pub.cs.diploma.RemoveRecursionBundle;
import ro.pub.cs.diploma.Util;

public abstract class DummyInspection extends BaseInspection {
  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return RemoveRecursionBundle.message(getKey());
  }

  @NotNull
  @Override
  protected String buildErrorString(Object... infos) {
    return RemoveRecursionBundle.message(getKey());
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new BaseInspectionVisitor() {
      @Override
      public void visitMethod(PsiMethod method) {
        super.visitMethod(method);
        registerMethodError(method);
      }
    };
  }

  @Nullable
  @Override
  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new InspectionGadgetsFix() {
      @Override
      protected void doFix(Project project, ProblemDescriptor descriptor) {
        final PsiMethod method = Util.getContainingMethod(descriptor.getPsiElement());
        if (method == null) {
          return;
        }
        IterativeMethodGenerator.getInstance(Util.getFactory(method), Util.getStyleManager(method), method, NameManager.getInstance(method))
          .createIterativeBody(getSteps());
      }

      @Nls
      @NotNull
      @Override
      public String getFamilyName() {
        return RemoveRecursionBundle.message(getKey());
      }
    };
  }

  protected abstract String getKey();
  protected abstract int getSteps();
}
