package ro.pub.cs.diploma.ir

import com.intellij.psi.PsiReturnStatement

class ReturnStatement(override val statement: PsiReturnStatement) : TerminatorStatement, WrapperStatement {
  override fun accept(visitor: Visitor) {
    visitor.visit(this)
  }
}
