package ro.pub.cs.diploma

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.GenericsUtil
import com.intellij.psi.PsiArrayAccessExpression
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiLabeledStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable
import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import com.intellij.psi.codeStyle.VariableKind
import com.siyeh.ig.PsiReplacementUtil
import com.siyeh.ig.psiutils.ParenthesesUtils
import org.jetbrains.annotations.NonNls

/**
 * @see com.siyeh.ipp.forloop.ReplaceForEachLoopWithIteratorForLoopIntention.processIntention
 */
fun replaceForEachLoopWithIteratorForLoop(statement: PsiForeachStatement, method: PsiMethod) {
    val iteratedValue = statement.iteratedValue ?: return
    if (iteratedValue.type !is PsiClassType) return
    @NonNls val methodCall = StringBuilder()
    if (ParenthesesUtils.getPrecedence(iteratedValue) > ParenthesesUtils.METHOD_CALL_PRECEDENCE) {
        methodCall.append('(').append(iteratedValue.text).append(')')
    } else {
        methodCall.append(iteratedValue.text)
    }
    methodCall.append(".iterator()")
    val iteratorCall = method.getFactory().createExpressionFromText(methodCall.toString(), iteratedValue)
    val variableType = GenericsUtil.getVariableTypeByExpressionType(iteratorCall.type) ?: return
    @NonNls val newStatement = StringBuilder()
    newStatement.append("for(").append(variableType.canonicalText).append(' ')
    val iterator = method.getStyleManager().suggestUniqueVariableName("iterator", method, true)
    newStatement.append(iterator).append("=").append(iteratorCall.text).append(';')
    newStatement.append(iterator).append(".hasNext();) {")
    if (JavaCodeStyleSettings.getInstance(statement.project).GENERATE_FINAL_LOCALS) {
        newStatement.append("final ")
    }
    val iterationParameter = statement.iterationParameter
    val typeText = iterationParameter.type.canonicalText
    newStatement.append(typeText).append(' ').append(iterationParameter.name).append(" = ").append(iterator).append(".next();")
    val body = statement.body ?: return
    if (body is PsiBlockStatement) {
        val block = body.codeBlock
        val children = block.children
        for (i in 1 until children.size - 1) {
            // skip the braces
            newStatement.append(children[i].text)
        }
    } else {
        newStatement.append(body.text)
    }
    newStatement.append('}')
    PsiReplacementUtil.replaceStatementAndShortenClassNames(statement, newStatement.toString())
}

/**
 * @see com.siyeh.ipp.forloop.ReplaceForEachLoopWithIndexedForLoopIntention.processIntention
 */
fun replaceForEachLoopWithIndexedForLoop(statement: PsiForeachStatement) {
    val iteratedValue = statement.iteratedValue ?: return
    val iteratedValueType = iteratedValue.type ?: return
    val isArray = iteratedValueType is PsiArrayType
    val grandParent = statement.parent
    val context = grandParent as? PsiLabeledStatement ?: statement
    val iteratedValueText = getReferenceToIterate(iteratedValue, context)
    @NonNls val newStatement = StringBuilder()
    val indexText = createVariableName("i", PsiType.INT, statement)
    createForLoopDeclaration(iteratedValue, isArray, iteratedValueText, newStatement, indexText)
    if (JavaCodeStyleSettings.getInstance(statement.project).GENERATE_FINAL_LOCALS) {
        newStatement.append("final ")
    }
    newStatement.append(statement.iterationParameter.type.canonicalText)
    newStatement.append(' ')
    newStatement.append(statement.iterationParameter.name)
    newStatement.append(" = ")
    newStatement.append(iteratedValueText)
    if (isArray) {
        newStatement.append('[')
        newStatement.append(indexText)
        newStatement.append("];")
    } else {
        newStatement.append(".get(")
        newStatement.append(indexText)
        newStatement.append(");")
    }
    val body = statement.body ?: return
    if (body is PsiBlockStatement) {
        val block = body.codeBlock
        val children = block.children
        for (i in 1 until children.size - 1) {
            // skip the braces
            newStatement.append(children[i].text)
        }
    } else {
        newStatement.append(body.text)
    }
    newStatement.append('}')
    PsiReplacementUtil.replaceStatementAndShortenClassNames(statement, newStatement.toString())
}

private fun createForLoopDeclaration(
    iteratedValue: PsiExpression,
    array: Boolean,
    iteratedValueText: String?,
    newStatement: StringBuilder,
    indexText: String
) {
    newStatement.append("for(int ")
    newStatement.append(indexText)
    newStatement.append(" = 0; ")
    newStatement.append(indexText)
    newStatement.append('<')
    if (iteratedValue is PsiTypeCastExpression) {
        newStatement.append('(')
        newStatement.append(iteratedValueText)
        newStatement.append(')')
    } else {
        newStatement.append(iteratedValueText)
    }
    if (array) {
        newStatement.append(".length")
    } else {
        newStatement.append(".size()")
    }
    newStatement.append(';')
    newStatement.append(indexText)
    newStatement.append("++)")
    newStatement.append("{ ")
}

private fun getVariableName(expression: PsiExpression): String? {
    when (expression) {
        is PsiMethodCallExpression -> {
            val methodExpression = expression.methodExpression
            val name = methodExpression.referenceName ?: return null
            return if (name.startsWith("to") && name.length > 2) {
                StringUtil.decapitalize(name.substring(2))
            } else if (name.startsWith("get") && name.length > 3) {
                StringUtil.decapitalize(name.substring(3))
            } else {
                name
            }
        }
        is PsiTypeCastExpression -> {
            return getVariableName(expression.operand ?: return null)
        }
        is PsiArrayAccessExpression -> {
            val name = getVariableName(expression.arrayExpression)
            return if (name == null) null else StringUtil.unpluralize(name)
        }
        is PsiParenthesizedExpression -> {
            return getVariableName(expression.expression ?: return null)
        }
        is PsiJavaCodeReferenceElement -> return expression.referenceName ?: (expression as PsiExpression).text
        else -> return null
    }
}

private fun getReferenceToIterate(expression: PsiExpression, context: PsiElement): String? {
    when (expression) {
        is PsiMethodCallExpression, is PsiTypeCastExpression, is PsiArrayAccessExpression, is PsiNewExpression -> {
            val variableName = getVariableName(expression)
            return createVariable(variableName, expression, context)
        }
        is PsiParenthesizedExpression -> {
            return getReferenceToIterate(expression.expression ?: return null, context)
        }
        is PsiJavaCodeReferenceElement -> {
            val referenceElement = expression as PsiJavaCodeReferenceElement
            val variableName = getVariableName(expression)
            if (referenceElement.isQualified) {
                return createVariable(variableName, expression, context)
            }
            val target = referenceElement.resolve()
            return if (target is PsiVariable) {
                // maybe should not do this for local variables outside of
                // anonymous classes
                variableName
            } else createVariable(variableName, expression, context)
        }
        else -> return expression.text
    }
}

private fun createVariable(variableNameRoot: String?, iteratedValue: PsiExpression, context: PsiElement): String? {
    val variableName = createVariableName(variableNameRoot ?: return null, iteratedValue)
    val iteratedValueType = iteratedValue.type ?: return null
    val declarationStatement = context.getFactory().createVariableDeclarationStatement(variableName, iteratedValueType, iteratedValue)
    val newElement = context.parent.addBefore(declarationStatement, context)
    context.getStyleManager().shortenClassReferences(newElement)
    return variableName
}

private fun createVariableName(baseName: String, assignedExpression: PsiExpression): String {
    val codeStyleManager = assignedExpression.getStyleManager()
    val names = codeStyleManager.suggestVariableName(VariableKind.LOCAL_VARIABLE, baseName, assignedExpression, null)
    return if (names.names.isEmpty()) {
        codeStyleManager.suggestUniqueVariableName(baseName, assignedExpression, true)
    } else codeStyleManager.suggestUniqueVariableName(names.names[0], assignedExpression, true)
}

private fun createVariableName(baseName: String, type: PsiType, context: PsiElement): String {
    val codeStyleManager = context.getStyleManager()
    val names = codeStyleManager.suggestVariableName(VariableKind.LOCAL_VARIABLE, baseName, null, type)
    return if (names.names.isEmpty()) {
        codeStyleManager.suggestUniqueVariableName(baseName, context, true)
    } else codeStyleManager.suggestUniqueVariableName(names.names[0], context, true)
}
