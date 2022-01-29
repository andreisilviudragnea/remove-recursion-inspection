package ro.pub.cs.diploma.ir

interface Visitor {
    fun visit(block: Block)

    fun visit(conditionalJumpStatement: ConditionalJumpStatement)

    fun visit(normalStatement: NormalStatement)

    fun visit(returnStatement: ReturnStatement)

    fun visit(unconditionalJumpStatement: UnconditionalJumpStatement)

    fun visit(switchStatement: SwitchStatement)
}
