package ro.pub.cs.diploma

import com.intellij.psi.*

fun PsiCodeBlock.extractReturnStatements(): List<PsiReturnStatement> {
  val returnStatements = ArrayList<PsiReturnStatement>()
  accept(object : JavaRecursiveElementWalkingVisitor() {
    override fun visitReturnStatement(statement: PsiReturnStatement) {
      super.visitReturnStatement(statement)
      returnStatements.add(statement)
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
  })
  return returnStatements
}
