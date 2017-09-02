package ro.pub.cs.diploma.inspections;

public class ReplaceForEachLoopsWithForLoopsInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "replace.foreach.loops.with.for.loops";
  }

  @Override
  protected int getSteps() {
    return 3;
  }
}
