package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import ro.pub.cs.diploma.*

fun addFrameClass(method: PsiMethod, nameManager: NameManager) {
  val variables = LinkedHashMap<String, PsiVariable>()
  method.accept(object : JavaRecursiveElementVisitor() {
    private fun processVariable(variable: PsiVariable) {
      variables.put(variable.name ?: return, variable)
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

  val factory = method.getFactory()
  val frameClassName = nameManager.frameClassName
  val frameClass = factory.createClass(frameClassName)

  // Set modifiers
  val modifierList = frameClass.modifierList ?: return
  modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
  modifierList.setModifierProperty(PsiModifier.STATIC, true)

  // Add fields
  val styleManager = method.getStyleManager()
  variables.entries
      .map {
        styleManager.shortenClassReferences(
            factory.createFieldFromText("private ${it.value.type.canonicalText} ${it.key};", null))
      }
      .forEach { frameClass.add(it) }
  frameClass.add(factory.createField(nameManager.blockFieldName, PsiPrimitiveType.INT))

  // Create constructor
  val constructor = factory.createConstructor(frameClassName)
  constructor.modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
  val body = constructor.body ?: return
  val parameterList = constructor.parameterList
  for (parameter in method.parameterList.parameters) {
    val name = parameter.name ?: return
    parameterList.add(factory.createParameter(name, parameter.type))
    body.add(factory.statement("this.$name = $name;"))
  }
  frameClass.add(constructor)

  // Add the nested class to the class of the method
  val containingClass = method.containingClass ?: return
  containingClass.addAfter(frameClass, method)
}