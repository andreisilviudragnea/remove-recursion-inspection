package ro.pub.cs.diploma.inspections

class ReplaceForEachLoopsWithForLoopsInspection : DummyInspection() {
  override val key
    get() = "replace.foreach.loops.with.for.loops"

  override val steps
    get() = 3
}
