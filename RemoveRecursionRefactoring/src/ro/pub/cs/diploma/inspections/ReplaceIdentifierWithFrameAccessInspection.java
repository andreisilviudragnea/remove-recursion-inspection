package ro.pub.cs.diploma.inspections;

public class ReplaceIdentifierWithFrameAccessInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "replace.identifier.with.frame.access";
  }

  @Override
  protected int getSteps() {
    return 7;
  }
}
