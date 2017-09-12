package ro.pub.cs.diploma

import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.JavaCodeStyleManager

class NameManager(private val method: PsiMethod) {
  private val styleManager: JavaCodeStyleManager = method.getStyleManager()

  val frameClassName: String = method.name.capitalize() + Constants.FRAME

  val switchLabelName: String = Constants.SWITCH_LABEL

  private fun getName(baseName: String): String = styleManager.suggestUniqueVariableName(baseName, method, true)

  val blockFieldName: String by lazy(LazyThreadSafetyMode.NONE) {
    getName(Constants.BLOCK_FIELD_NAME)
  }
  val frameVarName: String by lazy(LazyThreadSafetyMode.NONE) {
    getName(Constants.FRAME_VAR_NAME)
  }
  val stackVarName: String by lazy(LazyThreadSafetyMode.NONE) {
    getName(Constants.STACK_VAR_NAME)
  }
  val retVarName: String by lazy(LazyThreadSafetyMode.NONE) {
    getName(Constants.RET_VAR_NAME)
  }
}
