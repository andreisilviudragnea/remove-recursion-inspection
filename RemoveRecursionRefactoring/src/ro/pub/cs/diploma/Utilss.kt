package ro.pub.cs.diploma

import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Contract
import java.util.*

fun PsiElementFactory.statement(text: String) = this.createStatementFromText(text, null)

object Utilss {

  fun getFactory(element: PsiElement): PsiElementFactory = JavaPsiFacade.getElementFactory(element.project)

  fun getStyleManager(element: PsiElement): JavaCodeStyleManager = JavaCodeStyleManager.getInstance(element.project)

  fun getContainingMethod(element: PsiElement): PsiMethod? =
      PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, true, PsiClass::class.java, PsiLambdaExpression::class.java)

  @Contract(pure = true)
  fun getFrameClassName(methodName: String): String = methodName.capitalize() + Constants.FRAME

  fun isVoid(returnType: PsiType): Boolean = returnType is PsiPrimitiveType && PsiPrimitiveType.VOID == returnType

  fun <T : PsiElement> createPushStatement(factory: PsiElementFactory,
                                           frameClassName: String,
                                           stackVarName: String,
                                           arguments: Array<T>,
                                           function: (T) -> String): PsiStatement {
    val argumentsString = arguments.joinToString(",", transform = function)
    return factory.createStatementFromText("$stackVarName.push(new $frameClassName($argumentsString));", null)
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
