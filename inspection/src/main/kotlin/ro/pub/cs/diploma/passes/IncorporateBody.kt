package ro.pub.cs.diploma.passes

import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiWhileStatement
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.createPushStatement
import ro.pub.cs.diploma.getFactory
import ro.pub.cs.diploma.getStyleManager
import ro.pub.cs.diploma.statement

private fun PsiType.getInitialValue(): String = when (this) {
    PsiTypes.byteType() -> "(byte) 0"
    PsiTypes.shortType() -> "(short) 0"
    PsiTypes.intType() -> "0"
    PsiTypes.longType() -> "0L"
    PsiTypes.floatType() -> "0.0f"
    PsiTypes.doubleType() -> "0.0d"
    PsiTypes.charType() -> "'\u0000'"
    PsiTypes.booleanType() -> "false"
    else -> "null"
}

fun incorporateBody(method: PsiMethod, nameManager: NameManager): PsiCodeBlock? {
    val factory = method.getFactory()
    val body = method.body ?: return null
    val stackVarName = nameManager.stackVarName
    val frameClassName = nameManager.frameClassName

    val whileStatement = factory.statement(
        "while(!$stackVarName.isEmpty()) {" +
            "final $frameClassName ${nameManager.frameVarName} = $stackVarName.peek();" +
            "${body.text}}"
    ) as PsiWhileStatement

    val newBody = body.replace(factory.createCodeBlock()) as PsiCodeBlock

    val styleManager = method.getStyleManager()
    newBody.add(
        styleManager.shortenClassReferences(
            factory.statement(
                "final java.util.Deque<$frameClassName> $stackVarName = new java.util.ArrayDeque<>();"
            )
        )
    )
    newBody.add(factory.createPushStatement(frameClassName, stackVarName, method.parameterList.parameters) { it.name })

    val returnType = method.returnType ?: return null
    val retVarName = nameManager.retVarName
    if (returnType != PsiTypes.voidType()) {
        newBody.add(
            styleManager.shortenClassReferences(
                factory.statement(
                    "${returnType.canonicalText} $retVarName = ${returnType.getInitialValue()};"
                )
            )
        )
    }

    val incorporatedWhileStatement = newBody.add(whileStatement) as PsiWhileStatement

    if (returnType != PsiTypes.voidType()) {
        newBody.addAfter(factory.statement("return $retVarName;"), incorporatedWhileStatement)
    }

    val whileStatementBody = incorporatedWhileStatement.body as PsiBlockStatement? ?: return null
    val lastBodyStatement = whileStatementBody.codeBlock.lastBodyElement as PsiBlockStatement? ?: return null
    return lastBodyStatement.codeBlock
}
