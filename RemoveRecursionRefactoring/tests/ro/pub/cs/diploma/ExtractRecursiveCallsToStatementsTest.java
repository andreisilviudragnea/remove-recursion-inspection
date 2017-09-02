package ro.pub.cs.diploma;

import ro.pub.cs.diploma.inspections.ExtractRecursiveCallsToStatementsInspection;

public class ExtractRecursiveCallsToStatementsTest extends IGQuickFixesTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new ExtractRecursiveCallsToStatementsInspection());
    myRelativePath = "extract-recursive-calls-to-statements";
    myDefaultHint = RemoveRecursionBundle.message("extract.recursive.calls.to.statements");
  }

  public void testExtract1() {
    doTest();
  }

  public void testExtractForCondition() {
    doTest();
  }

  public void testExtractForEachValue() {
    doTest();
  }

  public void testExtractForInitializer1() {
    doTest();
  }

  public void testExtractForInitializer2() {
    doTest();
  }

  public void testExtractForUpdate() {
    doTest();
  }

  public void testExtractIfCondition() {
    doTest();
  }

  public void testExtractNested() {
    doTest();
  }

  public void testExtractNoChange1() {
    doTest();
  }

  public void testExtractNoChange2() {
    doTest();
  }

  public void testExtractSwitchCondition() {
    doTest();
  }

  public void testExtractWhileCondition() {
    doTest();
  }
}
