package ro.pub.cs.diploma.passes;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.Constants;
import ro.pub.cs.diploma.NameManager;
import ro.pub.cs.diploma.RecursionUtil;
import ro.pub.cs.diploma.Util;

import java.util.LinkedHashMap;
import java.util.Map;

public class AddFrameClass implements Pass<PsiMethod, Map<String, PsiVariable>, Object> {
  @NotNull private final PsiMethod myMethod;
  @NotNull private final NameManager myNameManager;

  private AddFrameClass(@NotNull final PsiMethod method, @NotNull final NameManager nameManager) {
    myMethod = method;
    myNameManager = nameManager;
  }

  @NotNull
  public static AddFrameClass getInstance(@NotNull final PsiMethod method, @NotNull final NameManager nameManager) {
    return new AddFrameClass(method, nameManager);
  }

  @Override
  public Map<String, PsiVariable> collect(PsiMethod method) {
    final Map<String, PsiVariable> variables = new LinkedHashMap<>();
    method.accept(new JavaRecursiveElementVisitor() {
      private void processVariable(PsiVariable variable) {
        variables.put(variable.getName(), variable);
      }

      @Override
      public void visitParameter(PsiParameter parameter) {
        if (!variables.containsKey(parameter.getName()) && RecursionUtil.hasToBeSavedOnStack(parameter, method)) {
          processVariable(parameter);
        }
      }

      @Override
      public void visitLocalVariable(PsiLocalVariable variable) {
        if (!variables.containsKey(variable.getName()) && RecursionUtil.hasToBeSavedOnStack(variable, method)) {
          processVariable(variable);
        }
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });
    return variables;
  }

  @Override
  public Object transform(Map<String, PsiVariable> variables) {
    final PsiElementFactory factory = Util.getFactory(myMethod);
    final String frameClassName = myNameManager.getFrameClassName();
    final PsiClass frameClass = factory.createClass(frameClassName);

    // Set modifiers
    final PsiModifierList modifierList = frameClass.getModifierList();
    if (modifierList == null) {
      return null;
    }
    modifierList.setModifierProperty(PsiModifier.PRIVATE, true);
    modifierList.setModifierProperty(PsiModifier.STATIC, true);

    // Add fields
    variables.entrySet()
      .stream()
      .map(pair -> factory
        .createFieldFromText("private " + pair.getValue().getType().getPresentableText() + " " + pair.getKey() + ";", null))
      .forEach(frameClass::add);
    frameClass.add(factory.createField(myNameManager.getBlockFieldName(), PsiPrimitiveType.INT));

    // Create constructor
    final PsiMethod constructor = factory.createConstructor(frameClassName);
    constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
    final PsiCodeBlock body = constructor.getBody();
    if (body == null) {
      return null;
    }
    final PsiParameterList parameterList = constructor.getParameterList();
    for (final PsiParameter parameter : myMethod.getParameterList().getParameters()) {
      final String name = parameter.getName();
      if (name == null) {
        return null;
      }
      parameterList.add(factory.createParameter(name, parameter.getType()));
      body.add(factory.createStatementFromText(Constants.THIS + "." + name + "=" + name + ";", null));
    }
    frameClass.add(constructor);

    // Add the nested class to the class of the method
    final PsiClass containingClass = myMethod.getContainingClass();
    if (containingClass == null) {
      return null;
    }
    containingClass.addAfter(frameClass, myMethod);

    return null;
  }
}
