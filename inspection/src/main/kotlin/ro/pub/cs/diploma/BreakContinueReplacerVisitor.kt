package ro.pub.cs.diploma

import com.intellij.openapi.util.Ref
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiBreakStatement
import com.intellij.psi.PsiContinueStatement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiStatement
import ro.pub.cs.diploma.ir.Block

class BreakContinueReplacerVisitor internal constructor(
    private val myBreakTargets: Map<PsiStatement, Block>,
    private val myContinueTargets: Map<PsiStatement, Block>,
    private val myFactory: PsiElementFactory,
    private val myCurrentBlock: Block,
) : JavaRecursiveElementVisitor() {
    private fun replaceWithUnconditionalJump(
        targetStatement: PsiStatement,
        targets: Map<PsiStatement, Block>,
        statement: PsiStatement,
    ) {
        val block = targets[targetStatement] ?: return
        myCurrentBlock.addEdgeTo(Ref.create(block))
        block.setDoNotInline(true)
        statement.parent.addBefore(myFactory.statement("frame.block = ${block.id};"), statement)
        statement.replace(myFactory.statement("break;"))
    }

    override fun visitBreakStatement(statement: PsiBreakStatement) {
        val exitedStatement = statement.findExitedStatement() ?: return
        replaceWithUnconditionalJump(exitedStatement, myBreakTargets, statement)
    }

    override fun visitContinueStatement(statement: PsiContinueStatement) {
        val continuedStatement = statement.findContinuedStatement() ?: return
        replaceWithUnconditionalJump(continuedStatement, myContinueTargets, statement)
    }
}
