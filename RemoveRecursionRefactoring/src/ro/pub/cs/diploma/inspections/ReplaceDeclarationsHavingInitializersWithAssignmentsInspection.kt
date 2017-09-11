package ro.pub.cs.diploma.inspections

class ReplaceDeclarationsHavingInitializersWithAssignmentsInspection : DummyInspection() {
  override val key
    get() = "replace.declarations.having.initializers.with.assignments"

  override val steps
    get() = 8
}
