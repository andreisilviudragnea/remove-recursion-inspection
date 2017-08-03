package ro.pub.cs.diploma;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/*
 * @see com.siyeh.ig.IGQuickFixesTestCase
 */
public abstract class IGQuickFixesTestCase extends LightCodeInsightFixtureTestCase {
  String myDefaultHint;
  String myRelativePath;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageLevelProjectExtension.getInstance(getProject()).setLanguageLevel(LanguageLevel.JDK_1_8);
  }

  @Override
  protected String getTestDataPath() {
    return "testdata";
  }

  void doTest() {
    doTest(getTestName(false));
  }

  private void doTest(final String testName) {
    myFixture.configureByFile(myRelativePath + "/" + testName + ".java");
    final IntentionAction action = myFixture.getAvailableIntention(myDefaultHint);
    assertNotNull(action);
    myFixture.launchAction(action);
    myFixture.checkResultByFile(myRelativePath + "/" + testName + ".after.java");
  }

  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return new DefaultLightProjectDescriptor();
  }
}
