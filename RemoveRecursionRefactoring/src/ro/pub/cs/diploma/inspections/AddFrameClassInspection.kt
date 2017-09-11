package ro.pub.cs.diploma.inspections

class AddFrameClassInspection : DummyInspection() {
  override val key
    get() = "add.frame.class"

  override val steps
    get() = 5
}
