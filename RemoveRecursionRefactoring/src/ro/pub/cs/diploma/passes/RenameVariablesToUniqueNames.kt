package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.refactoring.util.RefactoringUtil
import ro.pub.cs.diploma.*

import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * Rename all the variables (parameters and local variables) to unique names at method level (if necessary),
 * in order to avoid name clashes when generating the Frame class.
 */
class RenameVariablesToUniqueNames(private val myMethod: PsiMethod) : Pass<PsiMethod, Map<String, Map<PsiType, List<PsiVariable>>>, Nothing?> {

  override fun collect(method: PsiMethod): Map<String, Map<PsiType, List<PsiVariable>>> {
    val names = LinkedHashMap<String, MutableMap<PsiType, MutableList<PsiVariable>>>()
    method.accept(object : JavaRecursiveElementVisitor() {
      private fun processVariable(variable: PsiVariable) {
        val typesMap = names.getOrPut(variable.name ?: return) {
          LinkedHashMap()
        }
        val variables = typesMap.getOrPut(variable.type) {
          ArrayList()
        }
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
    return names
  }

  override fun transform(names: Map<String, Map<PsiType, List<PsiVariable>>>): Nothing? {
    val styleManager = myMethod.getStyleManager()
    for ((oldName, typesMap) in names) {
      if (typesMap.size <= 1) {
        continue
      }
      var first = true
      for (variables in typesMap.values) {
        if (first) {
          first = false
          continue
        }
        val newName = styleManager.suggestUniqueVariableName(oldName, myMethod, true)
        for (variable in variables) {
          RefactoringUtil.renameVariableReferences(variable, newName, LocalSearchScope(myMethod))
          variable.setName(newName)
        }
      }
    }
    return null
  }
}
