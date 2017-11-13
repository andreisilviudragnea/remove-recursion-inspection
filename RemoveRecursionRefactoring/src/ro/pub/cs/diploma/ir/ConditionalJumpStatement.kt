package ro.pub.cs.diploma.ir

import com.intellij.psi.PsiExpression

class ConditionalJumpStatement internal constructor(val condition: PsiExpression,
                                                    val thenBlock: Block,
                                                    val elseBlock: Block) : TerminatorStatement {
  override fun accept(visitor: Visitor) {
    visitor.visit(this)
  }
}
