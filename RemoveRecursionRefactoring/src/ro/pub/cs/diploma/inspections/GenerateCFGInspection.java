package ro.pub.cs.diploma.inspections;

public class GenerateCFGInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "generate.cfg";
  }

  @Override
  protected int getSteps() {
    return 9;
  }
}
