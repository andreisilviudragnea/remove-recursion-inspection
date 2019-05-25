package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import ro.pub.cs.diploma.*

fun extractRecursiveCallsToStatements(method: PsiMethod) {
  val returnType = method.returnType ?: return
  if (returnType == PsiPrimitiveType.VOID) {
    return
  }

  val calls = ArrayList<PsiMethodCallExpression>()
  method.accept(object : JavaRecursiveElementVisitor() {
    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
      super.visitMethodCallExpression(expression)
      if (expression.isRecursiveCallTo(method)) {
        calls.add(expression)
      }
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
  })

  val styleManager = method.getStyleManager()
  val factory = method.getFactory()
  calls@ for (call in calls) {
    val parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement::class.java, true)
    when (parentStatement) {
      is PsiDeclarationStatement -> {
        for (element in parentStatement.declaredElements) {
          if (element is PsiLocalVariable) {
            if (element.initializer === call) {
              continue@calls
            }
          }
        }
      }
      is PsiExpressionStatement -> {
        val expression = parentStatement.expression
        if (expression is PsiAssignmentExpression && expression.rExpression === call) {
          continue@calls
        }
        val parentForStatement = PsiTreeUtil.getParentOfType(call, PsiForStatement::class.java, true)
        if (parentForStatement?.update === parentStatement) {
          continue@calls
        }
      }
    }
    val parentStatementInBlock = call.parentStatementInBlock ?: continue@calls
    val parentBlock = parentStatementInBlock.parent as PsiCodeBlock
    val temp = styleManager.suggestUniqueVariableName(TEMP, method, true)
    parentBlock.addBefore(factory.createVariableDeclarationStatement(temp, returnType, call), parentStatementInBlock)
    call.replace(factory.expression(temp))
  }
}

private val PsiElement.parentStatementInBlock: PsiStatement?
  get() {
    var e = this
    while (true) {
      val parent = e.parent
      if (parent is PsiFile) {
        return null
      }
      if (parent is PsiStatement && parent.parent is PsiCodeBlock) {
        return parent
      }
      e = e.parent
    }
  }
