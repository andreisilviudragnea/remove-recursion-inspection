package ro.pub.cs.diploma;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class IterativeMethodGenerator {
    private static final String ITERATIVE = "Iterative";
    private static final String STACK = "stack";
    private static final String PUSH = "push";
    private static final String JAVA_UTIL_DEQUE = "java.util.Deque";
    private static final String JAVA_UTIL_LINKED_LIST = "java.util.LinkedList";
    private static final String NEW = "new";

    static PsiMethod createIterativeMethod(Project project, PsiElementFactory factory, PsiMethod oldMethod,
                                           List<Variable> variables) {
        final String name = oldMethod.getName();
        final String contextClassName = ContextClassGenerator.getContextClassName(name);
        final PsiType returnType = oldMethod.getReturnType();
        final PsiMethod method = factory.createMethod(name + ITERATIVE, returnType);

        copyParameters(oldMethod, method);
        copyModifiers(oldMethod, method);

        final PsiCodeBlock block = (PsiCodeBlock) oldMethod.getBody().copy();

        Visitors.replaceForEachStatementsWithForStatements(block);
        Visitors.replaceForStatementsWithWhileStatements(block);

        block.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitLocalVariable(PsiLocalVariable variable) {
                super.visitLocalVariable(variable);
                variables.add(new Variable(variable.getName(), variable.getType().getPresentableText()));
            }
        });

        Visitors.replaceSingleStatementsWithBlockStatements(factory, block);
        extractRecursiveCallsToStatements(factory, block, name, returnType, variables);

        replaceIdentifierWithContextAccess(factory, variables, block);
        replaceDeclarationsWithInitializersWithAssignments(factory, block);

        final BasicBlocksGenerator basicBlocksGenerator = new BasicBlocksGenerator(factory, name, contextClassName,
                returnType);
        block.accept(basicBlocksGenerator);
        final List<BasicBlocksGenerator.Pair> blocks = basicBlocksGenerator.getBlocks();

        blocks.forEach(codeBlock -> replaceReturnStatements(factory, codeBlock.getBlock()));

        final String casesString = blocks.stream()
                .map(section -> "case " + section.getId() + ": " + section.getBlock().getText())
                .collect(Collectors.joining(""));

        final PsiCodeBlock body = method.getBody();

        body.add(createStackDeclaration(project, factory, contextClassName));
        body.add(createPushStatement(factory, contextClassName, method));
        addRetDeclaration(factory, method);
        body.add(factory.createStatementFromText("while (true) {" +
                contextClassName + " context = stack.peek();" +
                "switch (context.section) {" + casesString + "} }", null));

        return method;
    }

    private static void replaceIdentifierWithContextAccess(PsiElementFactory factory, List<Variable> variables,
                                                           PsiCodeBlock block) {
        for (PsiReferenceExpression expression : Visitors.extractReferenceExpressions(block)) {
            final PsiElement element = expression.resolve();
            if (element instanceof PsiLocalVariable) {
                PsiLocalVariable variable = (PsiLocalVariable) element;
                if (variables.stream().anyMatch(variable1 -> variable1.getName().equals(variable.getName()))) {
                    final PsiElement nameElement = expression.getReferenceNameElement();
                    nameElement.replace(
                            factory.createExpressionFromText("context." + nameElement.getText(), null));
                }
            }
            if (element instanceof PsiParameter) {
                PsiParameter parameter = (PsiParameter) element;
                if (variables.stream().anyMatch(variable1 -> variable1.getName().equals(parameter.getName()))) {
                    final PsiElement nameElement = expression.getReferenceNameElement();
                    nameElement.replace(
                            factory.createExpressionFromText("context." + nameElement.getText(), null));
                }
            }
        }
    }

    private static void replaceReturnStatements(final PsiElementFactory factory, final PsiCodeBlock block) {
        for (final PsiReturnStatement statement : Visitors.extractReturnStatements(block)) {
            final PsiExpression returnValue = statement.getReturnValue();
            final boolean hasExpression = returnValue != null;
            final List<PsiStatement> statements = new ArrayList<>();
            if (hasExpression) {
                statements.add(factory.createStatementFromText("ret = " + returnValue.getText() + ";", null));
            }
            statements.add(factory.createStatementFromText("if (stack.size() == 1)\nreturn " +
                    (hasExpression ? "ret" : "") + "; else\nstack.pop();", null));
            statements.add(factory.createStatementFromText("break;", null));
            PsiElement anchor = statement;
            final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(statement, PsiCodeBlock.class, true);
            for (PsiStatement statement1 : statements) {
                anchor = parentBlock.addAfter(statement1, anchor);
            }
            statement.delete();
        }
    }

    private static void replaceDeclarationsWithInitializersWithAssignments(final PsiElementFactory factory,
                                                                           final PsiCodeBlock block) {
        for (final PsiDeclarationStatement statement : Visitors.extractDeclarationStatements(block)) {
            final PsiElement[] elements = statement.getDeclaredElements();
            final List<PsiStatement> assignments = new ArrayList<>();
            for (final PsiElement element : elements) {
                if (!(element instanceof PsiLocalVariable))
                    continue;
                final PsiLocalVariable variable = (PsiLocalVariable) element;
                if (!variable.hasInitializer())
                    continue;
                assignments.add(factory.createStatementFromText("context." +
                        variable.getName() + " = " + variable.getInitializer().getText() + ";", null));
            }
            final PsiElement parent = statement.getParent();
            if (!(parent instanceof PsiCodeBlock))
                continue;
            final PsiCodeBlock parentBlock = (PsiCodeBlock) parent;
            PsiElement anchor = statement;
            for (PsiStatement assignment : assignments) {
                anchor = parentBlock.addAfter(assignment, anchor);
            }
            statement.delete();
        }
    }

    private static void addRetDeclaration(PsiElementFactory factory, PsiMethod method) {
        final PsiType returnType = method.getReturnType();
        if (returnType instanceof PsiPrimitiveType && PsiPrimitiveType.VOID.equals(returnType))
            return;
        final PsiCodeBlock body = method.getBody();
        assert body != null;
        assert returnType != null;
        body.add(factory.createStatementFromText(returnType.getPresentableText() + " ret = 0;", null));
    }

    private static void extractRecursiveCallsToStatements(PsiElementFactory factory, PsiCodeBlock block, String name,
                                                          PsiType returnType, List<Variable> variables) {
        int count = 0;
        for (PsiMethodCallExpression call : Visitors.extractRecursiveCalls(block, name)) {
            final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement.class, true);
            if (parentStatement == call.getParent())
                continue;
            final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(call, PsiCodeBlock.class, true);
            final String temp = "temp" + count++;
            variables.add(new Variable(temp, returnType.getPresentableText()));
            parentBlock.addBefore(factory.createVariableDeclarationStatement(temp, returnType, call), parentStatement);
            call.replace(factory.createExpressionFromText(temp, null));
        }
    }

    @NotNull
    private static PsiStatement createPushStatement(@NotNull PsiElementFactory factory,
                                                    @NotNull String contextClassName,
                                                    @NotNull PsiMethod method) {
        final String arguments = Arrays.stream(method.getParameterList().getParameters())
                .map(PsiNamedElement::getName).collect(Collectors.joining(","));
        return factory.createStatementFromText(
                STACK + "." + PUSH + "(" + NEW + " " + contextClassName + "(" + arguments + "));", null);
    }

    @NotNull
    private static PsiElement createStackDeclaration(@NotNull Project project, @NotNull PsiElementFactory factory,
                                                     @NotNull String contextClassName) {
        final PsiStatement declarationStatement = factory.createStatementFromText(
                JAVA_UTIL_DEQUE + "<" + contextClassName + "> " + STACK + " = " +
                        NEW + " " + JAVA_UTIL_LINKED_LIST + "<>();", null);
        return JavaCodeStyleManager.getInstance(project).shortenClassReferences(declarationStatement);
    }

    private static void copyModifiers(PsiMethod oldMethod, PsiMethod method) {
        final PsiModifierList modifierList = oldMethod.getModifierList();
        for (String modifier : PsiModifier.MODIFIERS) {
            method.getModifierList().setModifierProperty(modifier, modifierList.hasExplicitModifier(modifier));
        }
    }

    private static void copyParameters(PsiMethod oldMethod, PsiMethod method) {
        for (PsiParameter psiParameter : oldMethod.getParameterList().getParameters()) {
            method.getParameterList().add(psiParameter);
        }
    }
}
