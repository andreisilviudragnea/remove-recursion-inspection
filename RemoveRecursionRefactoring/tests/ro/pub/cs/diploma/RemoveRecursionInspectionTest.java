package ro.pub.cs.diploma;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.siyeh.ig.LightInspectionTestCase;
import com.siyeh.ig.performance.TailRecursionInspectionTest;
import org.jetbrains.annotations.Nullable;

/**
 * @see TailRecursionInspectionTest
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
