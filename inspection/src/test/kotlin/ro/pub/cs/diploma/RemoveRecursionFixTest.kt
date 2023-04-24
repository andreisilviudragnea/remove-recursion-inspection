package ro.pub.cs.diploma

import org.junit.Ignore

/*
 * @see com.siyeh.ig.fixes.performance.RemoveTailRecursionFixTest
 */
@Ignore
class RemoveRecursionFixTest : IGQuickFixesTestCase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(RemoveRecursionInspection())
        myRelativePath = "fixes"
        myDefaultHint = RemoveRecursionBundle.message("remove.recursion.replace.quickfix")
    }

    fun testCallOnOtherInstance1() {
        doTest(trigger = false)
    }

    fun testCallOnOtherInstance2() {
        doTest(trigger = false)
    }

    fun testDependency1() {
        doTest()
    }

    fun testDependency2() {
        doTest()
    }

    fun testDependency3() {
        doTest()
    }

    fun testDependency4() {
        doTest()
    }

    fun testThisVariable() {
        doTest()
    }

    fun testUnmodifiedParameter() {
        doTest()
    }

    fun testArrayPrinter1() {
        doTest()
    }

    fun testArrayPrinter2() {
        doTest()
    }

    fun testArrayPrinter3() {
        doTest()
    }

    fun testFib1() {
        doTest()
    }

    fun testFib2() {
        doTest()
    }

    fun testFactorial1() {
        doTest()
    }

    fun testFactorial2() {
        doTest()
    }

    fun testBreakStatement() {
        doTest()
    }

    fun testLabeledBreakStatement() {
        doTest()
    }

    fun testNameClash() {
        doTest()
    }

    fun testContinueStatement() {
        doTest()
    }

    fun testDoWhileStatement() {
        doTest()
    }

    fun testP1() {
        doTest()
    }

    fun testLocalVariableSameName() {
        doTest()
    }

    fun testMergesort() {
        doTest()
    }

    fun testForeachArray() {
        doTest()
    }

    fun testBreakStatement1() {
        doTest()
    }

    fun testContinueStatement1() {
        doTest()
    }

    fun testFibSwitch() {
        doTest()
    }

    fun testFibSwitchFallThrough() {
        doTest()
    }
}
