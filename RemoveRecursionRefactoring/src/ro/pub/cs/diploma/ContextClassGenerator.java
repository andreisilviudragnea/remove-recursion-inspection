package ro.pub.cs.diploma;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class ContextClassGenerator {
    private static final String CONTEXT = "Context";
    private static final String THIS = "this";

    static PsiClass createContextClass(PsiElementFactory factory, PsiMethod method, List<Variable> variables) {
        final PsiClass psiClass = factory.createClass(getContextClassName(method.getName()));

        setModifiers(psiClass);
        addFields(factory, psiClass, variables);
        addConstructor(factory, psiClass, method.getParameterList().getParameters());

        return psiClass;
    }

    @NotNull
    static String getContextClassName(String methodName) {
        return Utilities.capitalize(methodName) + CONTEXT;
    }

    private static void addConstructor(PsiElementFactory factory, PsiClass psiClass, PsiParameter[] parameters) {
        final String className = psiClass.getName();
        assert className != null;
        final PsiMethod constructor = factory.createConstructor(className);
        constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
        final PsiCodeBlock body = constructor.getBody();
        assert body != null;
        for (PsiParameter parameter : parameters) {
            final String name = parameter.getName();
            assert name != null;
            constructor.getParameterList().add(factory.createParameter(name, parameter.getType()));
            body.add(factory.createStatementFromText(THIS + "." + name + " = " + name + ";", null));
        }
        psiClass.add(constructor);
    }

    private static void addFields(PsiElementFactory factory, PsiClass psiClass, List<Variable> variables) {
        for (Variable variable : variables) {
            psiClass.add(factory.createFieldFromText(
                    variable.getType() + " " + variable.getName() + ";", null));
        }
    }

    private static void setModifiers(PsiClass psiClass) {
        final PsiModifierList modifierList = psiClass.getModifierList();
        assert modifierList != null;
        modifierList.setModifierProperty(PsiModifier.PRIVATE, true);
        modifierList.setModifierProperty(PsiModifier.STATIC, true);
    }
}
