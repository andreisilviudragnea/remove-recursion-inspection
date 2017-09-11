package ro.pub.cs.diploma.inspections

class ExtractRecursiveCallsToStatementsInspection : DummyInspection() {
  override val key
    get() = "extract.recursive.calls.to.statements"

  override val steps
    get() = 4
}
