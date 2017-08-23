package ro.pub.cs.diploma.inspections;

public class ReplaceReturnStatementsInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "replace.return.statements";
  }

  @Override
  protected int getSteps() {
    return 13;
  }
}
