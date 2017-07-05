package ro.pub.cs.diploma;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.List;

class Visitors {
    private static final String SECTION = "section";

    static List<Variable> extractVariables(PsiMethod method) {
        List<Variable> variables = new ArrayList<>();
        method.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitParameter(PsiParameter parameter) {
                super.visitParameter(parameter);
                variables.add(new Variable(parameter.getName(), parameter.getType()));
            }

            @Override
            public void visitLocalVariable(PsiLocalVariable variable) {
                super.visitLocalVariable(variable);
                variables.add(new Variable(variable.getName(), variable.getType()));
            }
        });
        variables.add(new Variable(SECTION, PsiType.INT));
        return variables;
    }

    static List<PsiMethodCallExpression> extractRecursiveCalls(PsiCodeBlock block, String name) {
        final List<PsiMethodCallExpression> calls = new ArrayList<>();
        block.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                if (expression.getMethodExpression().getReferenceName().equals(name))
                    calls.add(expression);
            }
        });
        return calls;
    }

    static List<PsiDeclarationStatement> extractDeclarationStatements(PsiCodeBlock block) {
        final List<PsiDeclarationStatement> declarations = new ArrayList<>();
        block.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitDeclarationStatement(PsiDeclarationStatement statement) {
                super.visitDeclarationStatement(statement);
                declarations.add(statement);
            }
        });
        return declarations;
    }

    static List<PsiReturnStatement> extractReturnStatements(PsiCodeBlock block) {
        final List<PsiReturnStatement> returnStatements = new ArrayList<>();
        block.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitReturnStatement(PsiReturnStatement statement) {
                super.visitReturnStatement(statement);
                returnStatements.add(statement);
            }
        });
        return returnStatements;
    }

    static List<PsiIdentifier> extractIdentifiers(PsiCodeBlock block) {
        final List<PsiIdentifier> identifiers = new ArrayList<>();
        block.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitIdentifier(PsiIdentifier identifier) {
                super.visitIdentifier(identifier);
                identifiers.add(identifier);
            }
        });
        return identifiers;
    }

    static void replaceSingleStatementsWithBlockStatements(PsiElementFactory factory, PsiCodeBlock block) {
        block.accept(new JavaRecursiveElementWalkingVisitor() {
            private void replaceStatement(PsiStatement statement) {
                if (!(statement instanceof PsiBlockStatement)) {
                    statement.replace(factory.createStatementFromText("{" + statement.getText() + "}", null));
                }
            }

            @Override
            public void visitIfStatement(PsiIfStatement statement) {
                super.visitIfStatement(statement);
                replaceStatement(statement.getThenBranch());
                final PsiStatement elseBranch = statement.getElseBranch();
                if (elseBranch != null) {
                    replaceStatement(elseBranch);
                }
            }

            @Override
            public void visitForStatement(PsiForStatement statement) {
                super.visitForStatement(statement);
                replaceStatement(statement.getBody());
            }

            @Override
            public void visitWhileStatement(PsiWhileStatement statement) {
                super.visitWhileStatement(statement);
                replaceStatement(statement.getBody());
            }

            @Override
            public void visitDoWhileStatement(PsiDoWhileStatement statement) {
                super.visitDoWhileStatement(statement);
                replaceStatement(statement.getBody());

            }

            @Override
            public void visitForeachStatement(PsiForeachStatement statement) {
                super.visitForeachStatement(statement);
                replaceStatement(statement.getBody());
            }
        });
    }
}
