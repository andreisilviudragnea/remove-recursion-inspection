package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.containsInScopeRecursiveCallsTo
import ro.pub.cs.diploma.expression
import ro.pub.cs.diploma.getFactory

fun replaceReferencesWithFieldAccesses(method: PsiMethod,
                                       body: PsiCodeBlock,
                                       nameManager: NameManager) {
  val variables = ArrayList<PsiVariable>()
  variables.addAll(method.parameterList.parameters)

  body.accept(object : JavaRecursiveElementVisitor() {
    override fun visitLocalVariable(variable: PsiLocalVariable) {
      if (variable.containsInScopeRecursiveCallsTo(method)) {
        variables.add(variable)
      }
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
  })

  val qualifierExpression = method.getFactory().expression(nameManager.frameVarName)
  variables
      .flatMap { ReferencesSearch.search(it, LocalSearchScope(body)) }
      .filterIsInstance<PsiReferenceExpression>()
      .forEach { it.qualifierExpression = qualifierExpression }
}
