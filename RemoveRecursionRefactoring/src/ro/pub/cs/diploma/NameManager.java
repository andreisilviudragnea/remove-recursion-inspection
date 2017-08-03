package ro.pub.cs.diploma;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NameManager {
  @NotNull private final PsiMethod method;
  @NotNull private final JavaCodeStyleManager styleManager;

  @NotNull private final String frameClassName;

  @Nullable private String blockFieldName;
  @Nullable private String frameVarName;
  @Nullable private String stackVarName;
  @Nullable private String retVarName;

  @NotNull
  public static NameManager getInstance(@NotNull final PsiMethod method) {
    return new NameManager(method);
  }

  private NameManager(@NotNull PsiMethod method) {
    this.method = method;
    styleManager = Util.getStyleManager(method);
    frameClassName = Util.getFrameClassName(method.getName());
  }

  @NotNull
  private String getName(@NotNull final String baseName) {
    return styleManager.suggestUniqueVariableName(baseName, method, true);
  }

  @NotNull
  public String getFrameClassName() {
    return frameClassName;
  }

  @NotNull
  String getSwitchLabelName() {
    return Constants.SWITCH_LABEL;
  }

  @NotNull
  public String getBlockFieldName() {
    if (blockFieldName == null) {
      blockFieldName = getName(Constants.BLOCK_FIELD_NAME);
    }
    return blockFieldName;
  }

  @NotNull
  public String getFrameVarName() {
    if (frameVarName == null) {
      frameVarName = getName(Constants.FRAME_VAR_NAME);
    }
    return frameVarName;
  }

  @NotNull
  public String getStackVarName() {
    if (stackVarName == null) {
      stackVarName = getName(Constants.STACK_VAR_NAME);
    }
    return stackVarName;
  }

  @NotNull
  public String getRetVarName() {
    if (retVarName == null) {
      retVarName = getName(Constants.RET_VAR_NAME);
    }
    return retVarName;
  }
}
