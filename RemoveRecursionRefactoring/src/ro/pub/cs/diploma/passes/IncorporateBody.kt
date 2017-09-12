package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.Utilss
import ro.pub.cs.diploma.statement

class IncorporateBody(private val myNameManager: NameManager,
                      private val myFactory: PsiElementFactory,
                      private val myStyleManager: JavaCodeStyleManager) : Pass<PsiMethod, PsiMethod, PsiCodeBlock> {

  override fun collect(method: PsiMethod): PsiMethod = method

  private fun statement(text: String): PsiStatement = myFactory.statement(text)

  override fun transform(method: PsiMethod): PsiCodeBlock? {
    val body = method.body ?: return null
    val stackVarName = myNameManager.stackVarName
    val frameClassName = myNameManager.frameClassName
    val whileStatement = statement(
        "while(!$stackVarName.isEmpty()) {" +
            "final $frameClassName ${myNameManager.frameVarName} = $stackVarName.peek();" +
            "${body.text}}") as PsiWhileStatement

    val newBody = body.replace(myFactory.createCodeBlock()) as PsiCodeBlock

    newBody.add(myStyleManager.shortenClassReferences(statement(
        "final java.util.Deque<$frameClassName> $stackVarName = new java.util.ArrayDeque<>();")))
    newBody.add(Utilss.createPushStatement(myFactory, frameClassName, stackVarName,
        method.parameterList.parameters, { it.name ?: "" }))
    val returnType = method.returnType ?: return null
    val retVarName = myNameManager.retVarName
    if (returnType != PsiPrimitiveType.VOID) {
      newBody.add(myStyleManager.shortenClassReferences(statement(
          "${returnType.canonicalText} $retVarName=${getInitialValue(returnType)};")))
    }

    val incorporatedWhileStatement = newBody.add(whileStatement) as PsiWhileStatement

    if (returnType != PsiPrimitiveType.VOID) {
      newBody.addAfter(statement("return $retVarName;"), incorporatedWhileStatement)
    }

    val whileStatementBody = incorporatedWhileStatement.body as PsiBlockStatement? ?: return null
    val lastBodyStatement = whileStatementBody.codeBlock.lastBodyElement as PsiBlockStatement? ?: return null
    return lastBodyStatement.codeBlock
  }

  private fun getInitialValue(type: PsiType): String {
    if (PsiPrimitiveType.BYTE == type) {
      return "(byte) 0"
    }
    if (PsiPrimitiveType.SHORT == type) {
      return "(short) 0"
    }
    if (PsiPrimitiveType.INT == type) {
      return "0"
    }
    if (PsiPrimitiveType.LONG == type) {
      return "0L"
    }
    if (PsiPrimitiveType.FLOAT == type) {
      return "0.0f"
    }
    if (PsiPrimitiveType.DOUBLE == type) {
      return "0.0d"
    }
    if (PsiPrimitiveType.CHAR == type) {
      return "'\u0000'"
    }
    return if (PsiPrimitiveType.BOOLEAN == type) {
      "false"
    }
    else "null"
  }
}
