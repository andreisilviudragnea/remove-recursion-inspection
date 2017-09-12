package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import ro.pub.cs.diploma.*
import java.util.*

class AddFrameClass(private val myMethod: PsiMethod, private val myNameManager: NameManager) : Pass<PsiMethod, Map<String, PsiVariable>, Nothing?> {

  override fun collect(method: PsiMethod): Map<String, PsiVariable> {
    val variables = LinkedHashMap<String, PsiVariable>()
    method.accept(object : JavaRecursiveElementVisitor() {
      private fun processVariable(variable: PsiVariable) {
        val name = variable.name ?: return
        variables.put(name, variable)
      }

      override fun visitParameter(parameter: PsiParameter) {
        if (!variables.containsKey(parameter.name) && parameter.containsInScopeRecursiveCallsTo(method)) {
          processVariable(parameter)
        }
      }

      override fun visitLocalVariable(variable: PsiLocalVariable) {
        if (!variables.containsKey(variable.name) && variable.containsInScopeRecursiveCallsTo(method)) {
          processVariable(variable)
        }
      }

      override fun visitClass(aClass: PsiClass) {}

      override fun visitLambdaExpression(expression: PsiLambdaExpression) {}
    })
    return variables
  }

  override fun transform(variables: Map<String, PsiVariable>): Nothing? {
    val factory = myMethod.getFactory()
    val frameClassName = myNameManager.frameClassName
    val frameClass = factory.createClass(frameClassName)

    // Set modifiers
    val modifierList = frameClass.modifierList ?: return null
    modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
    modifierList.setModifierProperty(PsiModifier.STATIC, true)

    // Add fields
    val styleManager = myMethod.getStyleManager()
    variables.entries
        .map {
          styleManager.shortenClassReferences(
              factory.createFieldFromText("private ${it.value.type.canonicalText} ${it.key};", null))
        }
        .forEach { frameClass.add(it) }
    frameClass.add(factory.createField(myNameManager.blockFieldName, PsiPrimitiveType.INT))

    // Create constructor
    val constructor = factory.createConstructor(frameClassName)
    constructor.modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
    val body = constructor.body ?: return null
    val parameterList = constructor.parameterList
    for (parameter in myMethod.parameterList.parameters) {
      val name = parameter.name ?: return null
      parameterList.add(factory.createParameter(name, parameter.type))
      body.add(factory.statement("this.$name = $name;"))
    }
    frameClass.add(constructor)

    // Add the nested class to the class of the method
    val containingClass = myMethod.containingClass ?: return null
    containingClass.addAfter(frameClass, myMethod)

    return null
  }
}
