package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import com.intellij.refactoring.util.RefactoringUtil
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.containsInScopeRecursiveCallsTo
import ro.pub.cs.diploma.statement
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

class ReplaceDeclarationsHavingInitializersWithAssignments(private val myMethod: PsiMethod,
                                                           private val myNameManager: NameManager,
                                                           private val myFactory: PsiElementFactory) : Pass<PsiCodeBlock, List<PsiDeclarationStatement>, Any> {

  override fun collect(block: PsiCodeBlock): List<PsiDeclarationStatement> {
    val declarations = ArrayList<PsiDeclarationStatement>()
    block.accept(object : JavaRecursiveElementWalkingVisitor() {
      override fun visitDeclarationStatement(statement: PsiDeclarationStatement) {
        if (statement.containsInScopeRecursiveCallsTo(myMethod)) {
          declarations.add(statement)
        }
      }

      override fun visitClass(aClass: PsiClass) {}

      override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
    })
    return declarations
  }

  override fun transform(declarations: List<PsiDeclarationStatement>): Any? {
    for (statement in declarations) {
      val parent = statement.parent
      val stream = getVariablesStream(statement, myNameManager.frameVarName)
      if (parent is PsiForStatement) {
        statement.replace(myFactory.statement(stream.collect(Collectors.joining(",")) + ";"))
        continue
      }
      val parentBlock = parent as PsiCodeBlock
      var anchor: PsiElement = statement
      for (string in stream.collect(Collectors.toList())) {
        anchor = parentBlock.addAfter(myFactory.statement( "$string;"), anchor)
      }
      statement.delete()
    }
    return null
  }

  private fun getVariablesStream(statement: PsiDeclarationStatement, frameVarName: String): Stream<String> {
    return Arrays
        .stream(statement.declaredElements)
        .filter { element -> element is PsiLocalVariable }
        .map { element -> element as PsiLocalVariable }
        .filter({ it.hasInitializer() })
        .map { variable ->
          "$frameVarName.${variable.name} = ${RefactoringUtil.convertInitializerToNormalExpression(variable.initializer, variable.type).text}"
        }
  }
}
