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
import ro.pub.cs.diploma.RemoveRecursionBundle;
import ro.pub.cs.diploma.Util;
import ro.pub.cs.diploma.passes.ReplaceSingleStatementsWithBlockStatements;

public class ReplaceSingleStatementsWithBlockStatementsInspection extends BaseInspection {
  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return RemoveRecursionBundle.message("replace.single.statements.with.block.statements.diplay.name");
  }

  @NotNull
  @Override
  protected String buildErrorString(Object... infos) {
    return RemoveRecursionBundle.message("replace.single.statements.with.block.statements.problem.descriptor");
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
        ReplaceSingleStatementsWithBlockStatements.getInstance(Util.getFactory(method)).apply(method);
      }

      @Nls
      @NotNull
      @Override
      public String getFamilyName() {
        return RemoveRecursionBundle.message("replace.single.statements.with.block.statements.quickfix");
      }
    };
  }
}
