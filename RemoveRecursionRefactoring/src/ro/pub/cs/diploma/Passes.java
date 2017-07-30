package ro.pub.cs.diploma;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.refactoring.util.RefactoringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Passes {
  /**
   * Rename all the variables (parameters and local variables) to unique names at method level (if necessary),
   * in order to avoid name clashes when generating the Frame class.
   */
  static void renameVariablesToUniqueNames(@NotNull final PsiMethod method) {
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
        if (Util.hasToBeSavedOnStack(parameter, method)) {
          processVariable(parameter);
        }
      }

      @Override
      public void visitDeclarationStatement(PsiDeclarationStatement statement) {
        if (Util.hasToBeSavedOnStack(statement, method)) {
          Arrays
            .stream(statement.getDeclaredElements())
            .filter(element -> element instanceof PsiLocalVariable)
            .map(element -> (PsiLocalVariable)element)
            .forEach(this::processVariable);
        }
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

  private static Stream<String> getVariablesStream(PsiDeclarationStatement statement, String frameVarName) {
    return Arrays
      .stream(statement.getDeclaredElements())
      .filter(element -> element instanceof PsiLocalVariable)
      .map(element -> (PsiLocalVariable)element)
      .filter(PsiVariable::hasInitializer)
      .map(variable -> frameVarName + "." + variable.getName() + " = " + variable.getInitializer().getText());
  }

  static void replaceDeclarationsWithInitializersWithAssignments(@NotNull final String frameVarName,
                                                                 @NotNull final PsiMethod method,
                                                                 @NotNull final PsiCodeBlock block) {
    final List<PsiDeclarationStatement> declarations = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitDeclarationStatement(PsiDeclarationStatement statement) {
        if (Util.hasToBeSavedOnStack(statement, method)) {
          declarations.add(statement);
        }
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });
    final PsiElementFactory factory = Util.getFactory(block);
    for (final PsiDeclarationStatement statement : declarations) {
      final PsiElement parent = statement.getParent();
      final Stream<String> stream = getVariablesStream(statement, frameVarName);
      if (parent instanceof PsiForStatement) {
        statement.replace(Util.statement(factory, stream.collect(Collectors.joining(",")) + ";"));
        continue;
      }
      final PsiCodeBlock parentBlock = (PsiCodeBlock)parent;
      PsiElement anchor = statement;
      for (final String string : stream.collect(Collectors.toList())) {
        anchor = parentBlock.addAfter(Util.statement(factory, string + ";"), anchor);
      }
      statement.delete();
    }
  }
}
