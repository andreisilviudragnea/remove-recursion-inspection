package ro.pub.cs.diploma

import ro.pub.cs.diploma.inspections.ExtractRecursiveCallsToStatementsInspection

class ExtractRecursiveCallsToStatementsTest : IGQuickFixesTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ExtractRecursiveCallsToStatementsInspection())
    myRelativePath = "extract-recursive-calls-to-statements"
    myDefaultHint = RemoveRecursionBundle.message("extract.recursive.calls.to.statements")
  }

  fun testExtract1() {
    doTest()
  }

  fun testExtractForCondition() {
    doTest()
  }

  fun testExtractForEachValue() {
    doTest()
  }

  fun testExtractForInitializer1() {
    doTest()
  }

  fun testExtractForInitializer2() {
    doTest()
  }

  fun testExtractForUpdate() {
    doTest()
  }

  fun testExtractIfCondition() {
    doTest()
  }

  fun testExtractNested() {
    doTest()
  }

  fun testExtractNoChange1() {
    doTest()
  }

  fun testExtractNoChange2() {
    doTest()
  }

  fun testExtractSwitchCondition() {
    doTest()
  }

  fun testExtractWhileCondition() {
    doTest()
  }
}
