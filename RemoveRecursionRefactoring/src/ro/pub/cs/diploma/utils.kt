package ro.pub.cs.diploma

import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

fun PsiElementFactory.statement(text: String) = this.createStatementFromText(text, null)
fun <T : PsiElement> PsiElementFactory.createPushStatement(frameClassName: String,
                                                           stackVarName: String,
                                                           arguments: Array<T>,
                                                           function: (T) -> String): PsiStatement {
  val argumentsString = arguments.joinToString(",", transform = function)
  return this.statement("$stackVarName.push(new $frameClassName($argumentsString));")
}

fun PsiElement.getFactory(): PsiElementFactory = JavaPsiFacade.getElementFactory(this.project)
fun PsiElement.getStyleManager(): JavaCodeStyleManager = JavaCodeStyleManager.getInstance(this.project)
fun PsiElement.getContainingMethod() = PsiTreeUtil.getParentOfType(this, PsiMethod::class.java, true, PsiClass::class.java,
    PsiLambdaExpression::class.java)

fun PsiMethod.getPsiForEachStatements(): List<PsiForeachStatement> {
  val statements = ArrayList<PsiForeachStatement>()
  val method = this
  this.accept(object : JavaRecursiveElementVisitor() {
    override fun visitForeachStatement(statement: PsiForeachStatement) {
      super.visitForeachStatement(statement)
      if (statement.containsRecursiveCallsTo(method)) {
        statements.add(statement)
      }
    }
  })
  return statements
}
