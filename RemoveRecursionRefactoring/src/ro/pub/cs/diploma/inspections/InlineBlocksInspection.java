package ro.pub.cs.diploma.inspections;

public class InlineBlocksInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "inline.blocks";
  }

  @Override
  protected int getSteps() {
    return 12;
  }
}
