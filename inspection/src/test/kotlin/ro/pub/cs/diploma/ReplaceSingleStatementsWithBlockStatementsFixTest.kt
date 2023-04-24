package ro.pub.cs.diploma

import org.junit.Ignore
import ro.pub.cs.diploma.inspections.ReplaceSingleStatementsWithBlockStatementsInspection

@Ignore
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
