package ro.pub.cs.diploma

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor

/**
 * @see com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
 */
class DefaultLightProjectDescriptor : LightProjectDescriptor() {
    override fun getModuleTypeId() = ModuleTypeId.JAVA_MODULE

    override fun getSdk() = JavaSdk.getInstance().createJdk("java 1.8", "/Library/Java/JavaVirtualMachines/jdk1.8.0_241.jdk/Contents/Home", false)

    public override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
        val extension = model.getModuleExtension(LanguageLevelModuleExtension::class.java)
        if (extension != null) {
            extension.languageLevel = LanguageLevel.JDK_1_8
        }
    }
}
