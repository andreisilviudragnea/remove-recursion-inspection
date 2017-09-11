package ro.pub.cs.diploma.ir

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiExpression

class ConditionalJumpStatement internal constructor(val condition: PsiExpression,
                                                    private val thenBlockRef: Ref<Block>,
                                                    private val elseBlockRef: Ref<Block>) : TerminatorStatement {
  val thenBlock: Block
    get() = thenBlockRef.get()

  val elseBlock: Block
    get() = elseBlockRef.get()

  override fun accept(visitor: Visitor) {
    visitor.visit(this)
  }
}
