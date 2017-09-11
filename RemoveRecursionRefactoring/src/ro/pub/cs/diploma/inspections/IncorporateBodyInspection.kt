package ro.pub.cs.diploma.inspections

class IncorporateBodyInspection : DummyInspection() {
  override val key
    get() = "incorporate.body"

  override val steps
    get() = 6
}
