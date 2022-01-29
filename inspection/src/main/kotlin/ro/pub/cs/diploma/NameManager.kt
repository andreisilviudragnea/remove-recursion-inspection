package ro.pub.cs.diploma

import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import java.util.Locale

class NameManager(private val method: PsiMethod) {
    private val styleManager: JavaCodeStyleManager = method.getStyleManager()

    private fun getName(baseName: String): String =
        styleManager.suggestUniqueVariableName(baseName, method, true)

    val frameClassName: String = "${
    method.name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
    }$FRAME"

    val switchLabelName: String = SWITCH_LABEL

    val blockFieldName: String by lazy(LazyThreadSafetyMode.NONE) {
        getName(BLOCK_FIELD_NAME)
    }
    val frameVarName: String by lazy(LazyThreadSafetyMode.NONE) {
        getName(FRAME_VAR_NAME)
    }
    val stackVarName: String by lazy(LazyThreadSafetyMode.NONE) {
        getName(STACK_VAR_NAME)
    }
    val retVarName: String by lazy(LazyThreadSafetyMode.NONE) {
        getName(RET_VAR_NAME)
    }
}
