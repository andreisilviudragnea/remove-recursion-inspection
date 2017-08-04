package ro.pub.cs.diploma.inspections;

public class ReplaceSingleStatementsWithBlockStatementsInspection extends DummyInspection {
  @Override
  protected String getKey() {
    return "replace.single.statements.with.block.statements";
  }

  @Override
  protected int getSteps() {
    return 3;
  }
}
