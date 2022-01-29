package ro.pub.cs.diploma

import com.intellij.CommonBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.ResourceBundle

/**
 * @see com.siyeh.InspectionGadgetsBundle
 */
object RemoveRecursionBundle {
    @NonNls private const val BUNDLE = "RemoveRecursionBundle"

    private val bundle: ResourceBundle by lazy(LazyThreadSafetyMode.NONE) { ResourceBundle.getBundle(BUNDLE) }

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = CommonBundle.message(bundle, key, *params)
}
