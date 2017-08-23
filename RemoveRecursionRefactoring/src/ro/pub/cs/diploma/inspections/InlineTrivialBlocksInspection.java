package ro.pub.cs.diploma.inspections;

public class InlineTrivialBlocksInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "inline.trivial.blocks";
  }

  @Override
  protected int getSteps() {
    return 11;
  }
}
