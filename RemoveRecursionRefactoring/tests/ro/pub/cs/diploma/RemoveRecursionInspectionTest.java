package ro.pub.cs.diploma;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.siyeh.ig.LightInspectionTestCase;
import org.jetbrains.annotations.Nullable;

/*
 * @see com.siyeh.ig.performance.TailRecursionInspectionTest
 */
public class RemoveRecursionInspectionTest extends LightInspectionTestCase {
  @Nullable
  @Override
  protected InspectionProfileEntry getInspection() {
    return new RemoveRecursionInspection();
  }

  @Override
  protected String getTestDataPath() {
    return "testdata/inspection";
  }

  public void testLambdaWithReturnStmt() {
    doTest();
  }

  public void testTailRecursion() {
    doTest();
  }
}
