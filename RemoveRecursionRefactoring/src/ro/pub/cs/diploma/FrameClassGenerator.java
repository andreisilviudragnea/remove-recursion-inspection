package ro.pub.cs.diploma;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

class FrameClassGenerator {
  @Nullable
  static PsiClass createFrameClass(PsiElementFactory factory, PsiMethod method, String frameClassName, String blockFieldName) {
    @NotNull final PsiClass psiClass = factory.createClass(frameClassName);

    // Set modifiers
    @Nullable final PsiModifierList modifierList = psiClass.getModifierList();
    if (modifierList == null) {
      return null;
    }
    modifierList.setModifierProperty(PsiModifier.PRIVATE, true);
    modifierList.setModifierProperty(PsiModifier.STATIC, true);

    // Add fields
    final Map<String, String> variables = new LinkedHashMap<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitVariable(PsiVariable variable) {
        super.visitVariable(variable);
        final String name = variable.getName();
        if (name == null) {
          return;
        }
        final String typeText = variable.getType().getCanonicalText();
        if (variables.containsKey(name) && variables.get(name).equals(typeText)) {
          return;
        }
        variables.put(name, typeText);
      }
    });

    variables.entrySet().stream()
      .map(variable -> factory.createFieldFromText("private " + variable.getValue() + " " + variable.getKey() + ";", null))
      .forEach(psiClass::add);
    psiClass.add(factory.createField(blockFieldName, PsiPrimitiveType.INT));

    // Create constructor
    @NotNull final PsiMethod constructor = factory.createConstructor(frameClassName);
    constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
    @Nullable final PsiCodeBlock body = constructor.getBody();
    if (body == null) {
      return null;
    }
    @NotNull final PsiParameterList parameterList = constructor.getParameterList();
    for (@NotNull final PsiParameter parameter : method.getParameterList().getParameters()) {
      @Nullable final String name = parameter.getName();
      if (name == null) {
        return null;
      }
      parameterList.add(factory.createParameter(name, parameter.getType()));
      body.add(factory.createStatementFromText(Constants.THIS + "." + name + " = " + name + ";", null));
    }
    psiClass.add(constructor);

    return psiClass;
  }
}
