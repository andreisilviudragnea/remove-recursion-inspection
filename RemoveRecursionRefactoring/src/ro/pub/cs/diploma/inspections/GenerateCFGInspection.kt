package ro.pub.cs.diploma.inspections

class GenerateCFGInspection : DummyInspection() {
  override val key
    get() = "generate.cfg"

  override val steps
    get() = 9
}
