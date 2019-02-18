package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import com.intellij.refactoring.util.RefactoringUtil
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.containsInScopeRecursiveCallsTo
import ro.pub.cs.diploma.getFactory
import ro.pub.cs.diploma.statement

private fun PsiDeclarationStatement.getAssignments(frameVarName: String): List<String> {
  return declaredElements
      .filterIsInstance(PsiLocalVariable::class.java)
      .filter { it.hasInitializer() }
      .map { "$frameVarName.${it.name} = ${RefactoringUtil.convertInitializerToNormalExpression(it.initializer, it.type).text}" }
}

fun replaceDeclarationsHavingInitializersWithAssignments(method: PsiMethod, block: PsiCodeBlock, nameManager: NameManager) {
  val declarations = ArrayList<PsiDeclarationStatement>()

  block.accept(object : JavaRecursiveElementWalkingVisitor() {
    override fun visitDeclarationStatement(statement: PsiDeclarationStatement) {
      if (statement.containsInScopeRecursiveCallsTo(method)) {
        declarations.add(statement)
      }
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
  })

  val factory = method.getFactory()
  for (declaration in declarations) {
    val parent = declaration.parent
    val assignments = declaration.getAssignments(nameManager.frameVarName)
    when (parent) {
      is PsiForStatement -> {
        declaration.replace(factory.statement("${assignments.joinToString(",")};"))
      }
      is PsiCodeBlock -> {
        var anchor: PsiElement = declaration
        for (string in assignments) {
          anchor = parent.addAfter(factory.statement("$string;"), anchor)
        }
        declaration.delete()
      }
    }
  }
}
