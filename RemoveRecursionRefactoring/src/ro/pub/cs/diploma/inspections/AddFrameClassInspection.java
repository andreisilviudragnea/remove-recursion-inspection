package ro.pub.cs.diploma.inspections;

public class AddFrameClassInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "add.frame.class";
  }

  @Override
  protected int getSteps() {
    return 5;
  }
}
