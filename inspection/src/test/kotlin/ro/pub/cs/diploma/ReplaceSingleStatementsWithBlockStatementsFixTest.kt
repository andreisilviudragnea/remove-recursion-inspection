package ro.pub.cs.diploma

import ro.pub.cs.diploma.inspections.ReplaceSingleStatementsWithBlockStatementsInspection

class ReplaceSingleStatementsWithBlockStatementsFixTest : IGQuickFixesTestCase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(ReplaceSingleStatementsWithBlockStatementsInspection())
        myRelativePath = "replace-single-statements-with-block-statements"
        myDefaultHint = RemoveRecursionBundle.message("replace.single.statements.with.block.statements")
    }

    fun testReplaceSingleStatementsWithBlockStatements() {
        doTest()
    }
}
