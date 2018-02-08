package ro.pub.cs.diploma.inspections

class InlineBlocksInspection : DummyInspection() {
  override val key: String
    get() = "inline.blocks"

  override val steps: Int
    get() = 12
}
