package ro.pub.cs.diploma

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.testFramework.LightProjectDescriptor
import com.siyeh.ig.LightJavaInspectionTestCase

/*
 * @see com.siyeh.ig.performance.TailRecursionInspectionTest
 */
class RemoveRecursionInspectionTest : LightJavaInspectionTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = DefaultLightProjectDescriptor()

    override fun getInspection(): InspectionProfileEntry? = RemoveRecursionInspection()

    override fun getTestDataPath(): String = "src/test/resources/testdata/inspection"

    fun testLambdaWithReturnStmt() {
        doTest()
    }

    fun testTailRecursion() {
        doTest()
    }
}
