package ro.pub.cs.diploma

import com.intellij.psi.*

fun extractReturnStatements(block: PsiCodeBlock): List<PsiReturnStatement> {
  val returnStatements = ArrayList<PsiReturnStatement>()
  block.accept(object : JavaRecursiveElementWalkingVisitor() {
    override fun visitReturnStatement(statement: PsiReturnStatement) {
      super.visitReturnStatement(statement)
      returnStatements.add(statement)
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
  })
  return returnStatements
}
