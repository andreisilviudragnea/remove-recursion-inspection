package ro.pub.cs.diploma.inspections

class RenameVariablesToUniqueNamesInspection : DummyInspection() {
  override val key
    get() = "rename.variables.to.unique.names"

  override val steps
    get() = 2
}
