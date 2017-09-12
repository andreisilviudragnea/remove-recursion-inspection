package ro.pub.cs.diploma

import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Contract

fun PsiElementFactory.statement(text: String) = this.createStatementFromText(text, null)
fun PsiElement.getFactory(): PsiElementFactory = JavaPsiFacade.getElementFactory(this.project)
fun PsiElement.getStyleManager(): JavaCodeStyleManager = JavaCodeStyleManager.getInstance(this.project)

object Utilss {
  fun getContainingMethod(element: PsiElement): PsiMethod? =
      PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, true, PsiClass::class.java, PsiLambdaExpression::class.java)

  @Contract(pure = true)
  fun getFrameClassName(methodName: String): String = methodName.capitalize() + Constants.FRAME

  fun <T : PsiElement> createPushStatement(factory: PsiElementFactory,
                                           frameClassName: String,
                                           stackVarName: String,
                                           arguments: Array<T>,
                                           function: (T) -> String): PsiStatement {
    val argumentsString = arguments.joinToString(",", transform = function)
    return factory.statement("$stackVarName.push(new $frameClassName($argumentsString));")
  }

  fun getPsiForEachStatements(method: PsiMethod): List<PsiForeachStatement> {
    val statements = ArrayList<PsiForeachStatement>()
    method.accept(object : JavaRecursiveElementVisitor() {
      override fun visitForeachStatement(statement: PsiForeachStatement) {
        super.visitForeachStatement(statement)
        if (containsRecursiveCalls(statement, method)) {
          statements.add(statement)
        }
      }
    })
    return statements
  }
}
