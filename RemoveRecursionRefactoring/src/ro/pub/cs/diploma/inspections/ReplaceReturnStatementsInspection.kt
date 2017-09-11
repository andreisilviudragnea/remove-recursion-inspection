package ro.pub.cs.diploma.inspections

class ReplaceReturnStatementsInspection : DummyInspection() {
  override val key
    get() = "replace.return.statements"

  override val steps
    get() = 13
}
