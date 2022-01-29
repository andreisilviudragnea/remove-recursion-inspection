package ro.pub.cs.diploma.passes

import com.intellij.openapi.util.Ref
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiLoopStatement
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.util.PsiTreeUtil
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.getFactory
import ro.pub.cs.diploma.statement

private fun PsiCodeBlock.extractReturnStatements(): List<PsiReturnStatement> {
    val returnStatements = ArrayList<PsiReturnStatement>()
    accept(object : JavaRecursiveElementWalkingVisitor() {
        override fun visitReturnStatement(statement: PsiReturnStatement) {
            super.visitReturnStatement(statement)
            returnStatements.add(statement)
        }

        override fun visitClass(aClass: PsiClass) {}

        override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
    })
    return returnStatements
}

fun replaceReturnStatements(block: PsiCodeBlock, nameManager: NameManager, atLeastOneLabeledBreak: Ref<Boolean>) {
    val factory = block.getFactory()
    for (statement in block.extractReturnStatements()) {
        val returnValue = statement.returnValue
        val parentBlock = PsiTreeUtil.getParentOfType(statement, PsiCodeBlock::class.java, true) ?: continue
        var anchor: PsiElement = statement
        if (returnValue != null) {
            anchor = parentBlock.addAfter(factory.statement("${nameManager.retVarName} = ${returnValue.text};"), anchor)
        }
        anchor = parentBlock.addAfter(factory.statement("${nameManager.stackVarName}.pop();"), anchor)
        val inLoop = PsiTreeUtil.getParentOfType(statement, PsiLoopStatement::class.java, true, PsiClass::class.java) != null
        atLeastOneLabeledBreak.set(atLeastOneLabeledBreak.get() || inLoop)
        parentBlock.addAfter(factory.statement("break ${if (inLoop) nameManager.switchLabelName else ""};"), anchor)

        statement.delete()
    }
}
