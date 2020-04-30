package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import ro.pub.cs.diploma.*

private fun PsiType.getInitialValue(): String = when (this) {
  PsiPrimitiveType.BYTE -> "(byte) 0"
  PsiPrimitiveType.SHORT -> "(short) 0"
  PsiPrimitiveType.INT -> "0"
  PsiPrimitiveType.LONG -> "0L"
  PsiPrimitiveType.FLOAT -> "0.0f"
  PsiPrimitiveType.DOUBLE -> "0.0d"
  PsiPrimitiveType.CHAR -> "'\u0000'"
  PsiPrimitiveType.BOOLEAN -> "false"
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
          "${body.text}}") as PsiWhileStatement

  val newBody = body.replace(factory.createCodeBlock()) as PsiCodeBlock

  val styleManager = method.getStyleManager()
  newBody.add(styleManager.shortenClassReferences(factory.statement(
      "final java.util.Deque<$frameClassName> $stackVarName = new java.util.ArrayDeque<>();")))
  newBody.add(factory.createPushStatement(frameClassName, stackVarName, method.parameterList.parameters) { it.name })

  val returnType = method.returnType ?: return null
  val retVarName = nameManager.retVarName
  if (returnType != PsiPrimitiveType.VOID) {
    newBody.add(styleManager.shortenClassReferences(factory.statement(
        "${returnType.canonicalText} $retVarName = ${returnType.getInitialValue()};")))
  }

  val incorporatedWhileStatement = newBody.add(whileStatement) as PsiWhileStatement

  if (returnType != PsiPrimitiveType.VOID) {
    newBody.addAfter(factory.statement("return $retVarName;"), incorporatedWhileStatement)
  }

  val whileStatementBody = incorporatedWhileStatement.body as PsiBlockStatement? ?: return null
  val lastBodyStatement = whileStatementBody.codeBlock.lastBodyElement as PsiBlockStatement? ?: return null
  return lastBodyStatement.codeBlock
}
