package ro.pub.cs.diploma.ir

import com.intellij.openapi.util.Ref

class UnconditionalJumpStatement(val blockRef: Ref<Block>) : TerminatorStatement {
  val block: Block
    get() = blockRef.get()

  override fun accept(visitor: Visitor) {
    visitor.visit(this)
  }
}
