package ro.pub.cs.diploma.inspections;

public class ExtractRecursiveCallsToStatementsInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "extract.recursive.calls.to.statements";
  }

  @Override
  protected int getSteps() {
    return 4;
  }
}
