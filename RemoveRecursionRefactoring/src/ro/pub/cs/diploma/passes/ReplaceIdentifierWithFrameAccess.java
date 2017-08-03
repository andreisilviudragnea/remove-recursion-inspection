package ro.pub.cs.diploma.passes;

import com.intellij.psi.*;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.NameManager;
import ro.pub.cs.diploma.RecursionUtil;

import java.util.ArrayList;
import java.util.List;

public class ReplaceIdentifierWithFrameAccess implements Pass<PsiMethod, List<PsiVariable>, Object> {
  @NotNull private final NameManager myNameManager;
  @NotNull private final PsiElementFactory myFactory;
  @NotNull private final PsiCodeBlock myBody;

  private ReplaceIdentifierWithFrameAccess(@NotNull final NameManager nameManager,
                                           @NotNull final PsiElementFactory factory,
                                           @NotNull final PsiCodeBlock body) {
    myNameManager = nameManager;
    myFactory = factory;
    myBody = body;
  }

  @NotNull
  public static ReplaceIdentifierWithFrameAccess getInstance(@NotNull final NameManager nameManager,
                                                             @NotNull final PsiElementFactory factory,
                                                             @NotNull final PsiCodeBlock body) {
    return new ReplaceIdentifierWithFrameAccess(nameManager, factory, body);
  }

  @Override
  public List<PsiVariable> collect(PsiMethod method) {
    final List<PsiVariable> variables = new ArrayList<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitParameter(PsiParameter parameter) {
        if (RecursionUtil.hasToBeSavedOnStack(parameter, method)) {
          variables.add(parameter);
        }
      }

      @Override
      public void visitLocalVariable(PsiLocalVariable variable) {
        final String name = variable.getName();
        if (myNameManager.getFrameVarName().equals(name) || myNameManager.getStackVarName().equals(name)) {
          return;
        }
        if (RecursionUtil.hasToBeSavedOnStack(variable, method)) {
          variables.add(variable);
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
  public Object transform(List<PsiVariable> variables) {
    for (final PsiVariable variable : variables) {
      for (final PsiReference reference : ReferencesSearch.search(variable, new LocalSearchScope(myBody))) {
        if (reference instanceof PsiReferenceExpression) {
          ((PsiReferenceExpression)reference).setQualifierExpression(
            myFactory.createExpressionFromText(myNameManager.getFrameVarName(), null));
        }
      }
    }
    return null;
  }
}
