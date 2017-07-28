package ro.pub.cs.diploma;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

class FrameClassGenerator {
  static void addFrameClass(@NotNull final PsiMethod method, @NotNull final String frameClassName, @NotNull final String blockFieldName) {
    @NotNull final PsiElementFactory factory = Util.getFactory(method);
    @NotNull final PsiClass frameClass = factory.createClass(frameClassName);

    // Set modifiers
    @Nullable final PsiModifierList modifierList = frameClass.getModifierList();
    if (modifierList == null) {
      return;
    }
    modifierList.setModifierProperty(PsiModifier.PRIVATE, true);
    modifierList.setModifierProperty(PsiModifier.STATIC, true);

    // Add fields
    final Map<String, PsiType> variables = new LinkedHashMap<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitVariable(PsiVariable variable) {
        super.visitVariable(variable);
        final String name = variable.getName();
        if (name == null) {
          return;
        }
        final PsiType type = variable.getType();
        if (variables.containsKey(name) && variables.get(name).equals(type)) {
          return;
        }
        variables.put(name, type);
      }
    });

    variables.entrySet().stream()
      .map(variable -> factory
        .createFieldFromText("private " + variable.getValue().getPresentableText() + " " + variable.getKey() + ";", null))
      .forEach(frameClass::add);
    frameClass.add(factory.createField(blockFieldName, PsiPrimitiveType.INT));

    // Create constructor
    @NotNull final PsiMethod constructor = factory.createConstructor(frameClassName);
    constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
    @Nullable final PsiCodeBlock body = constructor.getBody();
    if (body == null) {
      return;
    }
    @NotNull final PsiParameterList parameterList = constructor.getParameterList();
    for (@NotNull final PsiParameter parameter : method.getParameterList().getParameters()) {
      @Nullable final String name = parameter.getName();
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
