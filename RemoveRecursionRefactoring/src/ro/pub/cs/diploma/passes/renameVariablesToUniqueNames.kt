package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.refactoring.util.RefactoringUtil
import ro.pub.cs.diploma.*

/**
 * Rename all the variables (parameters and local variables) to unique names at method level (if necessary),
 * in order to avoid name clashes when generating the Frame class.
 */
fun renameVariablesToUniqueNames(method: PsiMethod) {
  val names = LinkedHashMap<String, MutableMap<PsiType, MutableList<PsiVariable>>>()
  method.accept(object : JavaRecursiveElementVisitor() {
    private fun processVariable(variable: PsiVariable) {
      val typesMap = names.getOrPut(variable.name ?: return) { LinkedHashMap() }
      val variables = typesMap.getOrPut(variable.type) { ArrayList() }
      variables.add(variable)
    }

    override fun visitParameter(parameter: PsiParameter) {
      if (parameter.containsInScopeRecursiveCallsTo(method)) {
        processVariable(parameter)
      }
    }

    override fun visitLocalVariable(variable: PsiLocalVariable) {
      if (variable.containsInScopeRecursiveCallsTo(method)) {
        processVariable(variable)
      }
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
  })

  val styleManager = method.getStyleManager()
  for ((oldName, typesMap) in names) {
    for (variables in typesMap.values.drop(1)) {
      val newName = styleManager.suggestUniqueVariableName(oldName, method, true)
      for (variable in variables) {
        RefactoringUtil.renameVariableReferences(variable, newName, LocalSearchScope(method))
        variable.setName(newName)
      }
    }
  }
}
