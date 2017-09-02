package ro.pub.cs.diploma.inspections;

public class RenameVariablesToUniqueNamesInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "rename.variables.to.unique.names";
  }

  @Override
  protected int getSteps() {
    return 2;
  }
}
