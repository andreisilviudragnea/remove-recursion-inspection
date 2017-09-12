package ro.pub.cs.diploma.passes;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.refactoring.util.RefactoringUtil;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.RecursionUtilKt;
import ro.pub.cs.diploma.UtilssKt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rename all the variables (parameters and local variables) to unique names at method level (if necessary),
 * in order to avoid name clashes when generating the Frame class.
 */
public class RenameVariablesToUniqueNames implements Pass<PsiMethod, Map<String, Map<PsiType, List<PsiVariable>>>, Object> {
  @NotNull private final PsiMethod myMethod;

  private RenameVariablesToUniqueNames(@NotNull PsiMethod method) {
    myMethod = method;
  }

  @NotNull
  public static RenameVariablesToUniqueNames getInstance(@NotNull PsiMethod method) {
    return new RenameVariablesToUniqueNames(method);
  }

  @Override
  public Map<String, Map<PsiType, List<PsiVariable>>> collect(PsiMethod method) {
    final Map<String, Map<PsiType, List<PsiVariable>>> names = new LinkedHashMap<>();
    method.accept(new JavaRecursiveElementVisitor() {
      private void processVariable(PsiVariable variable) {
        final String name = variable.getName();
        if (name == null) {
          return;
        }
        if (!names.containsKey(name)) {
          names.put(name, new LinkedHashMap<>());
        }
        final Map<PsiType, List<PsiVariable>> typesMap = names.get(name);
        final PsiType type = variable.getType();
        if (!typesMap.containsKey(type)) {
          typesMap.put(type, new ArrayList<>());
        }
        final List<PsiVariable> variables = typesMap.get(type);
        variables.add(variable);
      }

      @Override
      public void visitParameter(PsiParameter parameter) {
        if (RecursionUtilKt.hasToBeSavedOnStack(parameter, method)) {
          processVariable(parameter);
        }
      }

      @Override
      public void visitLocalVariable(PsiLocalVariable variable) {
        if (RecursionUtilKt.hasToBeSavedOnStack(variable, method)) {
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
    return names;
  }

  @Override
  public Object transform(Map<String, Map<PsiType, List<PsiVariable>>> names) {
    final JavaCodeStyleManager styleManager = UtilssKt.getStyleManager(myMethod);
    for (final Map.Entry<String, Map<PsiType, List<PsiVariable>>> entry : names.entrySet()) {
      final Map<PsiType, List<PsiVariable>> typesMap = entry.getValue();
      if (typesMap.size() <= 1) {
        continue;
      }
      boolean first = true;
      for (final List<PsiVariable> variables : typesMap.values()) {
        if (first) {
          first = false;
          continue;
        }
        final String oldName = entry.getKey();
        final String newName = styleManager.suggestUniqueVariableName(oldName, myMethod, true);
        for (final PsiVariable variable : variables) {
          RefactoringUtil.renameVariableReferences(variable, newName, new LocalSearchScope(myMethod));
          variable.setName(newName);
        }
      }
    }
    return null;
  }
}
