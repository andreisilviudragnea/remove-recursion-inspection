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
    myDefaultHint = RemoveRecursionBundle.message("remove.recursion.replace.quickfix");
  }

  @Override
  protected String getTestDataPath() {
    return "testdata";
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
}
