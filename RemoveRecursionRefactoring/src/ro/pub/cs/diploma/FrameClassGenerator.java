package ro.pub.cs.diploma;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class FrameClassGenerator {
  @Nullable
  static PsiClass createFrameClass(PsiElementFactory factory, PsiMethod method, List<Variable> variables, String frameClassName) {
    @NotNull final PsiClass psiClass = factory.createClass(frameClassName);

    // Set modifiers
    @Nullable final PsiModifierList modifierList = psiClass.getModifierList();
    if (modifierList == null) {
      return null;
    }
    modifierList.setModifierProperty(PsiModifier.PRIVATE, true);
    modifierList.setModifierProperty(PsiModifier.STATIC, true);

    // Add fields
    variables.stream()
      .map(variable -> factory.createFieldFromText(variable.getType() + " " + variable.getName() + ";", null))
      .forEach(psiClass::add);

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
