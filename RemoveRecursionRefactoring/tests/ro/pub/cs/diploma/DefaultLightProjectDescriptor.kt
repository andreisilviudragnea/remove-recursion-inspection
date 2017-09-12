package ro.pub.cs.diploma

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor

/**
 * @see com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
 */
class DefaultLightProjectDescriptor : LightProjectDescriptor() {
  override fun getModuleType(): ModuleType<*> = StdModuleTypes.JAVA

  override fun getSdk(): Sdk? = JavaSdk.getInstance().createJdk("java 1.8", "/usr/lib/jvm/java-1.8.0-openjdk-amd64", false)

  public override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
    val extension = model.getModuleExtension(LanguageLevelModuleExtension::class.java)
    if (extension != null) {
      extension.languageLevel = LanguageLevel.JDK_1_8
    }
  }
}
