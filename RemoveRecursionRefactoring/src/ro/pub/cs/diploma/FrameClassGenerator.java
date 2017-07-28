package ro.pub.cs.diploma;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

class FrameClassGenerator {
  static void addFrameClass(@NotNull final PsiMethod method, @NotNull final String frameClassName, @NotNull final String blockFieldName) {
    final PsiElementFactory factory = Util.getFactory(method);
    final PsiClass frameClass = factory.createClass(frameClassName);

    // Set modifiers
    final PsiModifierList modifierList = frameClass.getModifierList();
    if (modifierList == null) {
      return;
    }
    modifierList.setModifierProperty(PsiModifier.PRIVATE, true);
    modifierList.setModifierProperty(PsiModifier.STATIC, true);

    // Add fields
    final Map<String, PsiVariable> variables = new LinkedHashMap<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitVariable(PsiVariable variable) {
        super.visitVariable(variable);
        final String name = variable.getName();
        if (!variables.containsKey(name)) {
          variables.put(name, variable);
        }
      }
    });

    variables.entrySet().stream()
      .map(pair -> factory
        .createFieldFromText("private " + pair.getValue().getType().getPresentableText() + " " + pair.getKey() + ";", null))
      .forEach(frameClass::add);
    frameClass.add(factory.createField(blockFieldName, PsiPrimitiveType.INT));

    // Create constructor
    final PsiMethod constructor = factory.createConstructor(frameClassName);
    constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
    final PsiCodeBlock body = constructor.getBody();
    if (body == null) {
      return;
    }
    final PsiParameterList parameterList = constructor.getParameterList();
    for (final PsiParameter parameter : method.getParameterList().getParameters()) {
      final String name = parameter.getName();
      if (name == null) {
        return;
      }
      parameterList.add(factory.createParameter(name, parameter.getType()));
      body.add(factory.createStatementFromText(Constants.THIS + "." + name + " = " + name + ";", null));
    }
    frameClass.add(constructor);

    final PsiClass containingClass = method.getContainingClass();
    if (containingClass == null) {
      return;
    }
    containingClass.addAfter(frameClass, method);
  }
}
