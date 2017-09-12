package ro.pub.cs.diploma.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls
import ro.pub.cs.diploma.IterativeMethodGenerator
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.RemoveRecursionBundle
import ro.pub.cs.diploma.Utilss

abstract class DummyInspection : BaseInspection() {
  protected abstract val key: String
  protected abstract val steps: Int

  @Nls
  override fun getDisplayName() = RemoveRecursionBundle.message(key)

  override fun buildErrorString(vararg infos: Any) = RemoveRecursionBundle.message(key)

  override fun buildVisitor(): BaseInspectionVisitor {
    return object : BaseInspectionVisitor() {
      override fun visitMethod(method: PsiMethod) {
        super.visitMethod(method)
        registerMethodError(method)
      }
    }
  }

  override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
    return object : InspectionGadgetsFix() {
      override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val method = Utilss.getContainingMethod(descriptor.psiElement) ?: return
        IterativeMethodGenerator.getInstance(Utilss.getFactory(method), Utilss.getStyleManager(method), method, NameManager.getInstance(method))
            .createIterativeBody(steps)
      }

      @Nls
      override fun getFamilyName() = RemoveRecursionBundle.message(key)
    }
  }
}
