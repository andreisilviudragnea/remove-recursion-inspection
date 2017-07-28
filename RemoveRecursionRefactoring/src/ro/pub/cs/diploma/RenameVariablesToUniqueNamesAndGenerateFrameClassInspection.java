package ro.pub.cs.diploma;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenameVariablesToUniqueNamesAndGenerateFrameClassInspection extends BaseInspection {
  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return RemoveRecursionBundle.message("rename.variables.to.unique.names.and.generate.frame.class.display.name");
  }

  @NotNull
  @Override
  protected String buildErrorString(Object... infos) {
    return RemoveRecursionBundle.message("rename.variables.to.unique.names.and.generate.frame.class.problem.descriptor");
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
        Passes.renameVariablesToUniqueNames(method);
        final JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(method.getProject());
        final String blockFieldName = styleManager.suggestUniqueVariableName(Constants.BLOCK_FIELD_NAME, method, true);
        FrameClassGenerator.addFrameClass(method, Util.getFrameClassName(method.getName()), blockFieldName);
      }

      @Nls
      @NotNull
      @Override
      public String getFamilyName() {
        return RemoveRecursionBundle.message("rename.variables.to.unique.names.and.generate.frame.class.quickfix");
      }
    };
  }
}
