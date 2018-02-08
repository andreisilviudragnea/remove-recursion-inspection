package ro.pub.cs.diploma.inspections

class RemoveUnreachableBlocksInspection : DummyInspection() {
  override val key
    get() = "remove.unreachable.blocks"

  override val steps
    get() = 10
}
