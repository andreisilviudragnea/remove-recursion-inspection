package ro.pub.cs.diploma;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.refactoring.util.RefactoringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Passes {
  /**
   * Rename all the variables (parameters and local variables) to unique names at method level (if necessary),
   * in order to avoid name clashes when generating the Frame class.
   */
  static void renameVariablesToUniqueNames(@NotNull final PsiMethod method) {
    final Map<String, Map<PsiType, List<PsiVariable>>> names = new LinkedHashMap<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitVariable(PsiVariable variable) {
        super.visitVariable(variable);
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
    });
    final JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(method.getProject());
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
        final String newName = styleManager.suggestUniqueVariableName(oldName, method, true);
        for (final PsiVariable variable : variables) {
          RefactoringUtil.renameVariableReferences(variable, newName, new LocalSearchScope(method));
          variable.setName(newName);
        }
      }
    }
  }
}
