package ro.pub.cs.diploma.inspections;

public class RemoveUnreachableBlocksInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "remove.unreachable.blocks";
  }

  @Override
  protected int getSteps() {
    return 10;
  }
}
