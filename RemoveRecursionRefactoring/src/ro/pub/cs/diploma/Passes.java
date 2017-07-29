package ro.pub.cs.diploma;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.refactoring.util.RefactoringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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

  static void replaceDeclarationsWithInitializersWithAssignments(@NotNull final String frameVarName,
                                                                 @NotNull final PsiCodeBlock block) {
    final PsiElementFactory factory = Util.getFactory(block);
    for (final PsiDeclarationStatement statement : Visitors.extractDeclarationStatements(block)) {
      final PsiElement parent = statement.getParent();
      if (parent instanceof PsiForStatement) {
        final String text = Arrays
                              .stream(statement.getDeclaredElements())
                              .map(element -> (PsiLocalVariable) element)
                              .filter(PsiVariable::hasInitializer)
                              .map(variable -> frameVarName + "." + variable.getName() + " = " + variable.getInitializer().getText())
                              .collect(Collectors.joining(",")) + ";";
        statement.replace(Util.statement(factory, text));
        continue;
      }
      final PsiCodeBlock parentBlock = (PsiCodeBlock)parent;
      PsiElement anchor = statement;
      for (final PsiElement element : statement.getDeclaredElements()) {
        final PsiLocalVariable variable = (PsiLocalVariable)element;
        final PsiExpression initializer = variable.getInitializer();
        if (initializer == null) {
          continue;
        }
        anchor = parentBlock.addAfter(
          Util.statement(factory, frameVarName + "." + variable.getName() + " = " + initializer.getText() + ";"), anchor);
      }
      statement.delete();
    }
  }
}
