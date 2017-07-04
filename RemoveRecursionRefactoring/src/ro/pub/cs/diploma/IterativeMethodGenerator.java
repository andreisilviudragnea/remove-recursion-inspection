package ro.pub.cs.diploma;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    static PsiMethod createIterativeMethod(Project project, PsiElementFactory factory, PsiMethod oldMethod,
                                           List<Variable> variables) {
        final String name = oldMethod.getName();
        final String contextClassName = ContextClassGenerator.getContextClassName(name);
        final PsiType returnType = oldMethod.getReturnType();
        final PsiMethod method = factory.createMethod(name + ITERATIVE, returnType);
        final PsiCodeBlock body = method.getBody();

        if (body == null)
            return null;

        final PsiCodeBlock block = factory.createCodeBlockFromText(oldMethod.getBody().getText(), null);

        Visitors.replaceSingleStatementsWithBlockStatements(factory, block);
        extractRecursiveCallsToStatements(factory, block, name, returnType);

        Visitors.extractVariables(oldMethod, block, variables);

        copyParameters(oldMethod, method);
        copyModifiers(oldMethod, method);

        replaceDeclarationsWithInitializersWithAssignments(factory, block);
        replaceIdentifierWithContextAccess(factory, variables, block);

        final BasicBlocksGenerator basicBlocksGenerator = new BasicBlocksGenerator(factory, name, contextClassName,
                returnType);
        block.accept(basicBlocksGenerator);
        final List<BasicBlocksGenerator.Pair> blocks = basicBlocksGenerator.getBlocks();
        for (BasicBlocksGenerator.Pair codeBlock : blocks) {
            replaceReturnStatements(factory, codeBlock.getBlock());
        }

//        simplifyIfStatement(factory, block);

//        final List<PsiCodeBlock> sections = extractSections(factory, block, name);

        body.add(createStackDeclaration(project, factory, contextClassName));
        body.add(createPushStatement(factory, contextClassName, method));
        addRetDeclaration(factory, method);

        final List<String> cases = new ArrayList<>();
        for (final BasicBlocksGenerator.Pair section : blocks) {
            //            replaceRecursiveCallsWithPushStatements(factory, name, contextClassName, section);

//            final PsiStatement[] statements = section.getStatements();
//            if (!(statements[statements.length - 1] instanceof PsiBreakStatement)) {
//                section.add(factory.createStatementFromText("break;", null));
//            }

            cases.add("case " + section.getId() + ": " + section.getBlock().getText());
        }

        body.add(factory.createStatementFromText("while (true) {" +
                contextClassName + " context = stack.peek();" +
                "switch (context.section) {" + String.join("", cases) +
                "} }", null));

//        for (PsiStatement statement: block.getStatements()) {
//            body.add(statement);
//        }
        return method;
    }

    private static void replaceRecursiveCallsWithPushStatements(PsiElementFactory factory, String name,
                                                                String contextClassName, PsiCodeBlock section) {
        for (PsiMethodCallExpression expression : Visitors.extractRecursiveCalls(section, name)) {
            List<PsiStatement> statements1 = new ArrayList<>();
            statements1.add(factory.createStatementFromText("context.section += 1;", null));
            final String s = Arrays.stream(expression.getArgumentList().getExpressions())
                    .map(PsiElement::getText).collect(Collectors.joining(","));
            statements1.add(factory.createStatementFromText("stack.push(new " +
                    contextClassName + "(" + s + "));", null));
            statements1.add(factory.createStatementFromText("break;", null));
            final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(expression, PsiStatement.class, true);
            final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(expression, PsiCodeBlock.class, true);
            PsiElement anchor = parentStatement;
            for (PsiStatement statement : statements1) {
                anchor = parentBlock.addAfter(statement, anchor);
            }
            parentStatement.delete();
        }
    }

    private static void replaceIdentifierWithContextAccess(PsiElementFactory factory, List<Variable> variables,
                                                           PsiCodeBlock block) {
        for (PsiIdentifier identifier : Visitors.extractIdentifiers(block)) {
            if (!inVariables(variables, identifier))
                continue;
            identifier.replace(factory.createExpressionFromText("context." + identifier.getText(), null));
        }
    }

    private static boolean inVariables(List<Variable> variables, PsiIdentifier identifier) {
        for (Variable variable : variables) {
            if (variable.getName().equals(identifier.getText())) {
                return true;
            }
        }
        return false;
    }

    private static void replaceReturnStatements(PsiElementFactory factory, PsiCodeBlock block) {
        for (PsiReturnStatement statement : Visitors.extractReturnStatements(block)) {
            final PsiExpression returnValue = statement.getReturnValue();
            final boolean hasExpression = returnValue != null;
            final List<PsiStatement> statements = new ArrayList<>();
            if (hasExpression) {
                statements.add(factory.createStatementFromText("ret = " + returnValue.getText() + ";",
                        null));
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

    private static void replaceDeclarationsWithInitializersWithAssignments(PsiElementFactory factory,
                                                                           PsiCodeBlock block) {
        final List<PsiDeclarationStatement> declarationStatements = Visitors.extractDeclarationStatements(block);
        for (PsiDeclarationStatement statement : declarationStatements) {
            final PsiElement[] elements = statement.getDeclaredElements();
            List<PsiStatement> assignments = new ArrayList<>();
            for (PsiElement element : elements) {
                if (!(element instanceof PsiLocalVariable))
                    continue;
                PsiLocalVariable variable = (PsiLocalVariable) element;
                if (!variable.hasInitializer())
                    continue;
                assignments.add(factory.createStatementFromText(
                        variable.getName() + " = " + variable.getInitializer().getText() + ";", null));
            }
            final PsiElement parent = statement.getParent();
            if (!(parent instanceof PsiCodeBlock))
                continue;
            PsiCodeBlock parentBlock = (PsiCodeBlock) parent;
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

    private static List<PsiCodeBlock> extractSections(PsiElementFactory factory, PsiCodeBlock block, String name) {
        final List<PsiCodeBlock> sections = new ArrayList<>();
        final PsiStatement[] bodyStatements = block.getStatements();
        int lastIndex = 0;

        for (PsiMethodCallExpression call : Visitors.extractRecursiveCalls(block, name)) {
            final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement.class, true);
            int index = 0;
            for (int i = 0; i < bodyStatements.length; i++) {
                if (bodyStatements[i] == parentStatement) {
                    index = i;
                    break;
                }
            }
            sections.add(factory.createCodeBlockFromText("{" + Arrays.asList(bodyStatements)
                    .subList(lastIndex, index + 1).stream()
                    .map(PsiElement::getText).collect(Collectors.joining("")) + "}", null));
            lastIndex = index + 1;
        }

        sections.add(factory.createCodeBlockFromText("{" + Arrays.asList(bodyStatements)
                .subList(lastIndex, bodyStatements.length).stream()
                .map(PsiElement::getText).collect(Collectors.joining("")) + "}", null));

        return sections;
    }

    private static void extractRecursiveCallsToStatements(PsiElementFactory factory, PsiCodeBlock block, String name,
                                                          PsiType returnType) {
        final List<PsiMethodCallExpression> calls = Visitors.extractRecursiveCalls(block, name);
        int count = 0;
        for (PsiMethodCallExpression call : calls) {
            final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement.class, true);
            final PsiElement parent = call.getParent();
            if (parentStatement == parent)
                continue;
            final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(call, PsiCodeBlock.class, true);
            assert parentBlock != null;
            final String temp = "temp" + count++;
            parentBlock.addBefore(factory.createVariableDeclarationStatement(temp, returnType, call), parentStatement);
            call.replace(factory.createExpressionFromText(temp, null));
        }
    }

    private static void simplifyIfStatement(PsiElementFactory factory, PsiCodeBlock codeBlock) {
        final PsiStatement[] statements = codeBlock.getStatements();
        final PsiStatement firstStatement = statements[0];
        if (statements.length != 1 || !(firstStatement instanceof PsiIfStatement))
            return;

        PsiIfStatement ifStatement = (PsiIfStatement) firstStatement;

        final PsiStatement thenBranch = ifStatement.getThenBranch();
        PsiElement thenElement = thenBranch;
        if (!(thenBranch instanceof PsiBlockStatement) && !(thenBranch instanceof PsiReturnStatement)) {
            final PsiCodeBlock block = factory.createCodeBlock();
            assert thenBranch != null;
            block.add(thenBranch);
            block.add(factory.createStatementFromText("return;", null));
            thenElement = thenBranch.replace(block);
        } else if (thenBranch instanceof PsiBlockStatement) {
            PsiBlockStatement blockStatement = (PsiBlockStatement) thenBranch;
            final PsiCodeBlock block = blockStatement.getCodeBlock();
            final PsiStatement[] blockStatements = block.getStatements();
            final PsiStatement lastStatement = blockStatements[blockStatements.length - 1];
            if (!(lastStatement instanceof PsiReturnStatement)) {
                block.add(factory.createStatementFromText("return;", null));
            }
        } else {
            final PsiCodeBlock block = factory.createCodeBlock();
            block.add(thenBranch);
            thenElement = thenBranch.replace(block);
        }

        final PsiStatement elseBranch = ifStatement.getElseBranch();
        if (elseBranch == null)
            return;

        if (elseBranch instanceof PsiBlockStatement) {
            PsiBlockStatement blockStatement = (PsiBlockStatement) elseBranch;
            final PsiCodeBlock block = blockStatement.getCodeBlock();
            final PsiStatement[] blockStatements = block.getStatements();
            for (PsiStatement statement : blockStatements) {
                codeBlock.add(statement);
            }
            if (!(blockStatements[blockStatements.length - 1] instanceof PsiReturnStatement)) {
                codeBlock.add(factory.createStatementFromText("return;", null));
            }
        } else {
            codeBlock.add(elseBranch);
        }

        final PsiExpression condition = ifStatement.getCondition();
        assert condition != null;
        final String text = thenElement.getText();
        final PsiIfStatement newIfStatement = (PsiIfStatement) factory.createStatementFromText(
                "if(" + condition.getText() + ")" + text, null);
        ifStatement.replace(newIfStatement);
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
