package ro.pub.cs.diploma.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.RemoveRecursionBundle
import ro.pub.cs.diploma.createIterativeBody
import ro.pub.cs.diploma.getContainingMethod

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
        val method = descriptor.psiElement.getContainingMethod() ?: return
        createIterativeBody(13 - steps, method, NameManager(method)).take(steps).forEach {  }
      }

      @Nls
      override fun getFamilyName() = RemoveRecursionBundle.message(key)
    }
  }
}
