package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.hasToBeSavedOnStack

class ReplaceIdentifierWithFrameAccess(private val myNameManager: NameManager,
                                       private val myFactory: PsiElementFactory,
                                       private val myBody: PsiCodeBlock) : Pass<PsiMethod, List<PsiVariable>, Any> {

  override fun collect(method: PsiMethod): List<PsiVariable> {
    val variables = ArrayList<PsiVariable>()
    method.accept(object : JavaRecursiveElementVisitor() {
      override fun visitParameter(parameter: PsiParameter) {
        if (hasToBeSavedOnStack(parameter, method)) {
          variables.add(parameter)
        }
      }

      override fun visitLocalVariable(variable: PsiLocalVariable) {
        val name = variable.name
        if (myNameManager.frameVarName == name || myNameManager.stackVarName == name) {
          return
        }
        if (hasToBeSavedOnStack(variable, method)) {
          variables.add(variable)
        }
      }

      override fun visitClass(aClass: PsiClass) {}

      override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
    })
    return variables
  }

  override fun transform(variables: List<PsiVariable>): Any? {
    variables
        .flatMap { ReferencesSearch.search(it, LocalSearchScope(myBody)) }
        .filterIsInstance<PsiReferenceExpression>()
        .forEach { it.qualifierExpression = myFactory.createExpressionFromText(myNameManager.frameVarName, null) }
    return null
  }
}
