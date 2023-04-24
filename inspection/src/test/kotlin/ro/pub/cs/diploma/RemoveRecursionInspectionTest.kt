package ro.pub.cs.diploma

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.testFramework.LightProjectDescriptor
import com.siyeh.ig.LightJavaInspectionTestCase
import org.junit.Ignore

/*
 * @see com.siyeh.ig.performance.TailRecursionInspectionTest
 */
@Ignore
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
