package ro.pub.cs.diploma

import com.intellij.codeInsight.daemon.impl.RecursiveCallLineMarkerProvider
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.siyeh.ig.performance.TailRecursionInspection
import com.siyeh.ig.psiutils.ParenthesesUtils
import java.util.*

/**
 * Checks if the `expression` is a recursive method call to `method`.
 *
 * @see RecursiveCallLineMarkerProvider.isRecursiveMethodCall
 * @see TailRecursionInspection.TailRecursionVisitor.visitReturnStatement
 * @see com.siyeh.ig.psiutils.RecursionUtils
 * @see com.siyeh.ig.psiutils.RecursionVisitor
 */
fun PsiMethodCallExpression.isRecursiveCallTo(method: PsiMethod): Boolean {
  val methodExpression = this.methodExpression
  if (method.name != methodExpression.referenceName) {
    return false
  }
  val calledMethod = this.resolveMethod()
  if (method != calledMethod) {
    return false
  }
  if (method.hasModifierProperty(PsiModifier.STATIC) || method.hasModifierProperty(PsiModifier.PRIVATE)) {
    return true
  }
  val qualifier = ParenthesesUtils.stripParentheses(methodExpression.qualifierExpression)
  return qualifier == null || qualifier is PsiThisExpression
}


/**
 * Returns true if the specified `element` contains at least on recursive call to the specified `method`.
 */
fun containsRecursiveCalls(element: PsiElement, method: PsiMethod): Boolean {
  val contains = Ref(false)
  element.accept(object : JavaRecursiveElementWalkingVisitor() {
    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
      super.visitMethodCallExpression(expression)
      if (expression.isRecursiveCallTo(method)) {
        contains.set(true)
        stopWalking()
      }
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
  })
  return contains.get()
}

fun extractStatementsContainingRecursiveCalls(incorporatedBody: PsiCodeBlock,
                                              method: PsiMethod): Set<PsiStatement> {
  val recursiveCalls = ArrayList<PsiMethodCallExpression>()
  incorporatedBody.accept(object : JavaRecursiveElementVisitor() {
    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
      super.visitMethodCallExpression(expression)
      if (expression.isRecursiveCallTo(method)) {
        recursiveCalls.add(expression)
      }
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
  })

  val statementsContainingRecursiveCalls = HashSet<PsiStatement>()
  for (call in recursiveCalls) {
    var parent = call.parent
    while (parent !== incorporatedBody) {
      if (parent is PsiStatement) {
        statementsContainingRecursiveCalls.add(parent)
      }
      parent = parent.parent
    }
  }

  return statementsContainingRecursiveCalls
}

private fun getElementsInScope(parameter: PsiParameter): List<PsiElement> {
  val parent = parameter.parent
  val elements = ArrayList<PsiElement>()

  if (parent is PsiParameterList) {
    val body = (parent.getParent() as PsiMethod).body
    if (body != null) {
      elements.add(body)
    }
  }
  else if (parent is PsiForeachStatement) {
    val body = parent.body
    if (body != null) {
      elements.add(body)
    }
  }

  return elements
}

fun hasToBeSavedOnStack(parameter: PsiParameter, method: PsiMethod): Boolean =
    getElementsInScope(parameter).any { containsRecursiveCalls(it, method) }

private fun getElementsInScope(variable: PsiLocalVariable): List<PsiElement> {
  val declarationStatement = variable.parent as PsiDeclarationStatement
  val parent = declarationStatement.parent

  val elements = ArrayList<PsiElement>()

  // This is because the variable is actually used only after it has been initialized.
  //final PsiExpression initializer = variable.getInitializer();
  //if (initializer != null) {
  //  elements.add(initializer);
  //}

  var met = false
  for (element in declarationStatement.declaredElements) {
    if (element is PsiLocalVariable) {
      if (met) {
        elements.add(element)
      }
      if (element === variable) {
        met = true
      }
    }
  }

  if (parent is PsiForStatement) {
    val condition = parent.condition
    if (condition != null) {
      elements.add(condition)
    }
    val update = parent.update
    if (update != null) {
      elements.add(update)
    }
    val body = parent.body
    if (body != null) {
      elements.add(body)
    }
  }
  else if (parent is PsiCodeBlock) {

    met = false
    for (psiStatement in parent.statements) {
      if (met) {
        elements.add(psiStatement)
      }
      if (psiStatement === declarationStatement) {
        met = true
      }
    }
  }

  return elements
}

fun hasToBeSavedOnStack(variable: PsiLocalVariable, method: PsiMethod): Boolean =
    getElementsInScope(variable).any { containsRecursiveCalls(it, method) }

fun hasToBeSavedOnStack(statement: PsiDeclarationStatement, method: PsiMethod): Boolean =
    statement.declaredElements.any { it is PsiLocalVariable && hasToBeSavedOnStack(it, method) }
