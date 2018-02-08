package ro.pub.cs.diploma.inspections

class ReplaceIdentifierWithFrameAccessInspection : DummyInspection() {
  override val key
    get() = "replace.identifier.with.frame.access"

  override val steps
    get() = 7
}
