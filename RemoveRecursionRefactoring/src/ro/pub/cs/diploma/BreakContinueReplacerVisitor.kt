package ro.pub.cs.diploma

import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import ro.pub.cs.diploma.ir.Block

class BreakContinueReplacerVisitor internal constructor(private val myBreakTargets: Map<PsiStatement, Block>,
                                                        private val myContinueTargets: Map<PsiStatement, Block>,
                                                        private val myFactory: PsiElementFactory,
                                                        private val myCurrentBlock: Block) : JavaRecursiveElementVisitor() {

  private fun replaceWithUnconditionalJump(targetStatement: PsiStatement,
                                           targets: Map<PsiStatement, Block>,
                                           statement: PsiStatement) {
    val block = targets[targetStatement] ?: return
    myCurrentBlock.addEdgeTo(Ref.create(block))
    block.setDoNotInline(true)
    statement.parent.addBefore(myFactory.createStatementFromText("frame.block = ${block.id};", null), statement)
    statement.replace(myFactory.createStatementFromText("break;", null))
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
