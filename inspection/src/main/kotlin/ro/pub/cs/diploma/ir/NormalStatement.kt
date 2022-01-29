package ro.pub.cs.diploma.ir

import com.intellij.psi.PsiStatement

class NormalStatement(override val statement: PsiStatement) : Statement, WrapperStatement {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
