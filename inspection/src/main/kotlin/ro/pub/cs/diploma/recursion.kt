package ro.pub.cs.diploma

import com.intellij.codeInsight.daemon.impl.RecursiveCallLineMarkerProvider
import com.intellij.openapi.util.Ref
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiStatement
import com.intellij.psi.PsiThisExpression
import com.siyeh.ig.performance.TailRecursionInspection
import com.siyeh.ig.psiutils.ParenthesesUtils

/**
 * Checks if the expression is a recursive method call to [method].
 *
 * @see RecursiveCallLineMarkerProvider.isRecursiveMethodCall
 * @see TailRecursionInspection.TailRecursionVisitor.visitReturnStatement
 * @see com.siyeh.ig.psiutils.RecursionUtils
 * @see com.siyeh.ig.psiutils.RecursionVisitor
 */
fun PsiMethodCallExpression.isRecursiveCallTo(method: PsiMethod): Boolean {
    val methodExpression = methodExpression
    if (method.name != methodExpression.referenceName) {
        return false
    }
    val calledMethod = resolveMethod()
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
 * Returns true if the element contains at least on recursive call to [method].
 */
fun PsiElement.containsRecursiveCallsTo(method: PsiMethod): Boolean {
    val contains = Ref(false)
    accept(object : JavaRecursiveElementWalkingVisitor() {
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

fun PsiCodeBlock.extractStatementsContainingRecursiveCallsTo(method: PsiMethod): Set<PsiStatement> {
    val recursiveCalls = ArrayList<PsiMethodCallExpression>()
    accept(object : JavaRecursiveElementVisitor() {
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
        while (parent !== this) {
            if (parent is PsiStatement) {
                statementsContainingRecursiveCalls.add(parent)
            }
            parent = parent.parent
        }
    }

    return statementsContainingRecursiveCalls
}

fun PsiParameter.containsInScopeRecursiveCallsTo(method: PsiMethod): Boolean =
    getElementsInScope().any { it.containsRecursiveCallsTo(method) }

fun PsiLocalVariable.containsInScopeRecursiveCallsTo(method: PsiMethod): Boolean =
    getElementsInScope().any { it.containsRecursiveCallsTo(method) }

fun PsiDeclarationStatement.containsInScopeRecursiveCallsTo(method: PsiMethod): Boolean =
    declaredElements.any { it is PsiLocalVariable && it.containsInScopeRecursiveCallsTo(method) }
