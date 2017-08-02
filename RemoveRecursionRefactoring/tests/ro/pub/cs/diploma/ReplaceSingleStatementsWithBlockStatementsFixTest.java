package ro.pub.cs.diploma;

import ro.pub.cs.diploma.inspections.ReplaceSingleStatementsWithBlockStatementsInspection;

public class ReplaceSingleStatementsWithBlockStatementsFixTest extends IGQuickFixesTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new ReplaceSingleStatementsWithBlockStatementsInspection());
    myRelativePath = "replace-single-statements-with-block-statements";
    myDefaultHint = RemoveRecursionBundle.message("replace.single.statements.with.block.statements.quickfix");
  }

  @Override
  protected String getTestDataPath() {
    return "testdata";
  }

  public void testReplaceSingleStatementsWithBlockStatements() {
    doTest();
  }
}
