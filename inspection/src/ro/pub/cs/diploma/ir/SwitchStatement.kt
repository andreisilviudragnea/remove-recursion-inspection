package ro.pub.cs.diploma.ir

import com.intellij.psi.PsiExpression

class SwitchStatement internal constructor(val expression: PsiExpression,
                                           val statements: List<Statement>) : TerminatorStatement {
  override fun accept(visitor: Visitor) {
    visitor.visit(this)
  }
}
