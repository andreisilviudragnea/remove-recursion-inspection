package ro.pub.cs.diploma.inspections;

public class ReplaceDeclarationsHavingInitializersWithAssignmentsInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "replace.declarations.having.initializers.with.assignments";
  }

  @Override
  protected int getSteps() {
    return 8;
  }
}
