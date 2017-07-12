package ro.pub.cs.diploma;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * @see com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
 */
public class DefaultLightProjectDescriptor extends LightProjectDescriptor {
  @NotNull
  @Override
  public ModuleType getModuleType() {
    return StdModuleTypes.JAVA;
  }

  @Override
  public Sdk getSdk() {
    return JavaSdk.getInstance().createJdk("java 1.8", "/usr/lib/jvm/java-1.8.0-openjdk-amd64", false);
  }

  @Override
  public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
    LanguageLevelModuleExtension extension = model.getModuleExtension(LanguageLevelModuleExtension.class);
    if (extension != null) {
      extension.setLanguageLevel(LanguageLevel.HIGHEST);
    }
  }
}
