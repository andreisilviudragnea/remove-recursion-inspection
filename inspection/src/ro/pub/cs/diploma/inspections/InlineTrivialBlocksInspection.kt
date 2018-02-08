package ro.pub.cs.diploma.inspections

class InlineTrivialBlocksInspection : DummyInspection() {
  override val key
    get() = "inline.trivial.blocks"

  override val steps
    get() = 11
}
