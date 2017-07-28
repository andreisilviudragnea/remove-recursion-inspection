package ro.pub.cs.diploma;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.refactoring.util.RefactoringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Passes {
  /**
   * Rename all the variables (parameters and local variables) to unique names at method level (if necessary),
   * in order to avoid name clashes when generating the Frame class.
   */
  static void renameVariablesToUniqueNames(JavaCodeStyleManager styleManager, PsiMethod method) {
    final Map<String, Map<String, List<PsiVariable>>> names = new LinkedHashMap<>();
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
        final Map<String, List<PsiVariable>> typesMap = names.get(name);
        final String typeText = variable.getType().getCanonicalText();
        if (!typesMap.containsKey(typeText)) {
          typesMap.put(typeText, new ArrayList<>());
        }
        final List<PsiVariable> variables = typesMap.get(typeText);
        variables.add(variable);
      }
    });
    for (Map.Entry<String, Map<String, List<PsiVariable>>> entry : names.entrySet()) {
      final Map<String, List<PsiVariable>> typesMap = entry.getValue();
      if (typesMap.size() <= 1) {
        continue;
      }
      boolean first = true;
      for (List<PsiVariable> variables : typesMap.values()) {
        if (first) {
          first = false;
          continue;
        }
        final String oldName = entry.getKey();
        final String newName = styleManager.suggestUniqueVariableName(oldName, method, true);
        for (PsiVariable variable : variables) {
          RefactoringUtil.renameVariableReferences(variable, newName, new LocalSearchScope(method));
          variable.setName(newName);
        }
      }
    }
  }
}
