package ro.pub.cs.diploma

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.testFramework.LightProjectDescriptor
import com.siyeh.ig.LightInspectionTestCase

/*
 * @see com.siyeh.ig.performance.TailRecursionInspectionTest
 */
class RemoveRecursionInspectionTest : LightInspectionTestCase() {
  override fun getProjectDescriptor(): LightProjectDescriptor = DefaultLightProjectDescriptor()

  override fun getInspection(): InspectionProfileEntry? = RemoveRecursionInspection()

  override fun getTestDataPath(): String = "testdata/inspection"

  fun testLambdaWithReturnStmt() {
    doTest()
  }

  fun testTailRecursion() {
    doTest()
  }
}
