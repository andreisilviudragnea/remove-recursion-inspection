package ro.pub.cs.diploma

import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import junit.framework.TestCase

/*
 * @see com.siyeh.ig.IGQuickFixesTestCase
 */
abstract class IGQuickFixesTestCase : LightCodeInsightFixtureTestCase() {
    internal var myDefaultHint: String? = null
    internal var myRelativePath: String? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        LanguageLevelProjectExtension.getInstance(project).languageLevel = LanguageLevel.JDK_1_8
    }

    override fun getTestDataPath(): String = "src/test/resources/testdata"

    internal fun doTest(trigger: Boolean = true) {
        doTest(getTestName(false), trigger)
    }

    private fun doTest(testName: String, trigger: Boolean) {
        myFixture.configureByFile("$myRelativePath/$testName.java")
        if (trigger) {
            val action = myFixture.getAvailableIntention(myDefaultHint ?: return)
            TestCase.assertNotNull(action)
            myFixture.launchAction(action!!)
        }
        myFixture.checkResultByFile("$myRelativePath/$testName.after.java")
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = DefaultLightProjectDescriptor()
}
