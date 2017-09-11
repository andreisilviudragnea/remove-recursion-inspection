package ro.pub.cs.diploma.inspections

class ReplaceSingleStatementsWithBlockStatementsInspection : DummyInspection() {
  override val key
    get() = "replace.single.statements.with.block.statements"

  override val steps
    get() = 1
}
