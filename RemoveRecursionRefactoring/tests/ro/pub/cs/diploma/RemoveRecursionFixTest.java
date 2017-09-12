package ro.pub.cs.diploma;

/*
 * @see com.siyeh.ig.fixes.performance.RemoveTailRecursionFixTest
 */
public class RemoveRecursionFixTest extends IGQuickFixesTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new RemoveRecursionInspection());
    myRelativePath = "fixes";
    myDefaultHint = RemoveRecursionBundle.INSTANCE.message("remove.recursion.replace.quickfix");
  }

  public void testCallOnOtherInstance1() {
    doTest();
  }

  public void testCallOnOtherInstance2() {
    doTest();
  }

  public void testDependency1() {
    doTest();
  }

  public void testDependency2() {
    doTest();
  }

  public void testDependency3() {
    doTest();
  }

  public void testDependency4() {
    doTest();
  }

  public void testThisVariable() {
    doTest();
  }

  public void testUnmodifiedParameter() {
    doTest();
  }

  public void testArrayPrinter1() {
    doTest();
  }

  public void testArrayPrinter2() {
    doTest();
  }

  public void testArrayPrinter3() {
    doTest();
  }

  public void testFib1() {
    doTest();
  }

  public void testFib2() {
    doTest();
  }

  public void testFactorial1() {
    doTest();
  }

  public void testFactorial2() {
    doTest();
  }

  public void testBreakStatement() {
    doTest();
  }

  public void testLabeledBreakStatement() {
    doTest();
  }

  public void testNameClash() {
    doTest();
  }

  public void testContinueStatement() {
    doTest();
  }

  public void testDoWhileStatement() {
    doTest();
  }

  public void testP1() {
    doTest();
  }

  public void testLocalVariableSameName() {
    doTest();
  }

  public void testMergesort() {
    doTest();
  }

  public void testForeachArray() {
    doTest();
  }

  public void testBreakStatement1() {
    doTest();
  }

  public void testContinueStatement1() {
    doTest();
  }

  public void testFibSwitch() {
    doTest();
  }

  public void testFibSwitchFallThrough() {
    doTest();
  }
}
