package ro.pub.cs.diploma

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import com.siyeh.ig.performance.TailRecursionInspection

/**
 * @see TailRecursionInspection
 */
class RemoveRecursionInspection : BaseInspection() {
  override fun getDisplayName(): String = RemoveRecursionBundle.message("remove.recursion.display.name")

  override fun buildErrorString(vararg infos: Any): String = RemoveRecursionBundle.message("remove.recursion.problem.descriptor")

  override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
    val containingMethod = infos[0] as PsiMethod
    return if (!mayBeReplacedByIterativeMethod(containingMethod)) {
      null
    }
    else object : InspectionGadgetsFix() {
      override fun getFamilyName(): String = RemoveRecursionBundle.message("remove.recursion.replace.quickfix")

      public override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val method = descriptor.psiElement.getContainingMethod() ?: return
        IterativeMethodGenerator.getInstance(method.getFactory(), method.getStyleManager(), method, NameManager(method))
            .createIterativeBody(13)
      }
    }
  }

  private fun mayBeReplacedByIterativeMethod(containingMethod: PsiMethod): Boolean {
    if (containingMethod.isVarArgs) {
      return false
    }
    val parameters = containingMethod.parameterList.parameters
    return parameters.none { it.hasModifierProperty(PsiModifier.FINAL) }
  }

  override fun buildVisitor(): BaseInspectionVisitor {
    return object : BaseInspectionVisitor() {
      override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        super.visitMethodCallExpression(expression)
        val method = expression.getContainingMethod() ?: return
        if (expression.isRecursiveCallTo(method)) {
          registerMethodCallError(expression, method)
        }
      }
    }
  }
}