package ro.pub.cs.diploma;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BasicBlocksGenerator extends JavaRecursiveElementVisitor {
    public class Pair {
        private PsiCodeBlock block;
        private int id;

        public Pair(PsiCodeBlock block, int id) {
            this.block = block;
            this.id = id;
        }

        PsiCodeBlock getBlock() {
            return block;
        }

        int getId() {
            return id;
        }
    }


    private List<Pair> blocks = new ArrayList<>();
    private Pair currentPair;
    private PsiStatement currentStatement;
    private PsiElementFactory factory;
    private int blockCounter;
    //    private int currentJumpIndex;
    private String methodName;
    private String contextClassName;
    private PsiType returnType;

    BasicBlocksGenerator(PsiElementFactory factory, String methodName, String contextClassName, PsiType returnType) {
        this.factory = factory;
        currentPair = new Pair(factory.createCodeBlock(), blockCounter++);
        blocks.add(currentPair);
        this.methodName = methodName;
        this.contextClassName = contextClassName;
        this.returnType = returnType;
    }

    @Override
    public void visitCodeBlock(PsiCodeBlock block) {
        super.visitCodeBlock(block);

        final PsiStatement[] statements = block.getStatements();
        // This is a hack, this method gets called only for the method block, not for blocks of block statements.
        if (PsiPrimitiveType.VOID.equals(returnType) && !(statements[statements.length - 1] instanceof PsiReturnStatement)) {
            currentPair.getBlock().add(factory.createStatementFromText("return;", null));
        }
    }

    @Override
    public void visitDeclarationStatement(PsiDeclarationStatement statement) {
        currentStatement = (PsiStatement) currentPair.getBlock().add(statement);
        super.visitDeclarationStatement(statement);
    }

    @Override
    public void visitExpressionStatement(PsiExpressionStatement statement) {
//        super.visitExpressionStatement(statement);
        currentStatement = (PsiStatement) currentPair.getBlock().add(statement);
        statement.getExpression().accept(this);
    }

    @Override
    public void visitIfStatement(PsiIfStatement statement) {
//        super.visitIfStatement(statement);
        final Pair thenPair = new Pair(factory.createCodeBlock(), blockCounter++);
        blocks.add(thenPair);
        Pair elsePair = null;

        final PsiStatement elseBranch = statement.getElseBranch();
        String elseJump = "";
        if (elseBranch != null) {
            elsePair = new Pair(factory.createCodeBlock(), blockCounter++);
            blocks.add(elsePair);
            elseJump = "else\ncontext.section = " + elsePair.getId() + ";";
        }

        final Pair mergePair = new Pair(factory.createCodeBlock(), blockCounter++);
        blocks.add(mergePair);
//        currentJumpIndex = mergePair.getId();
        if (elseBranch == null) {
            elseJump = "else\ncontext.section = " + mergePair.getId() + ";";
        }
        currentPair.getBlock().add(factory.createStatementFromText("if (" + statement.getCondition().getText() +
                ")\ncontext.section = " + thenPair.getId() + ";" + elseJump, null));
        currentPair.getBlock().add(factory.createStatementFromText("break;", null));

        currentPair = thenPair;
        final PsiStatement thenBranch = statement.getThenBranch();
        thenBranch.accept(this);
        final PsiStatement[] thenStatements = currentPair.getBlock().getStatements();
        if (thenStatements.length == 0 || !(thenStatements[thenStatements.length - 1] instanceof PsiReturnStatement)) {
            currentPair.getBlock().add(factory.createStatementFromText("context.section = " + mergePair.getId() + ";", null));
            currentPair.getBlock().add(factory.createStatementFromText("break;", null));
        }

        if (elseBranch != null) {
            currentPair = elsePair;
            elseBranch.accept(this);
            final PsiStatement[] elseStatements = currentPair.getBlock().getStatements();
            if (elseStatements.length == 0 || !(elseStatements[elseStatements.length - 1] instanceof PsiReturnStatement)) {
                currentPair.getBlock().add(factory.createStatementFromText("context.section = " + mergePair.getId() + ";", null));
                currentPair.getBlock().add(factory.createStatementFromText("break;", null));
            }
        }

        currentPair = mergePair;
    }

    @Override
    public void visitBlockStatement(PsiBlockStatement statement) {
//        super.visitBlockStatement(statement);
        final PsiStatement[] statements = statement.getCodeBlock().getStatements();
        for (PsiStatement statement1 : statements) {
            statement1.accept(this);
        }
//        if (statements[statements.length - 1] instanceof PsiReturnStatement)
//            return;
//        currentPair.getBlock().add(factory.createStatementFromText("context.section = " + currentJumpIndex + ";", null));
//        currentPair.getBlock().add(factory.createStatementFromText("break;", null));
    }

    @Override
    public void visitReturnStatement(PsiReturnStatement statement) {
//        super.visitReturnStatement(statement);
        currentPair.getBlock().add(statement);
    }

    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
//        super.visitMethodCallExpression(expression);
        if (expression.getMethodExpression().getReferenceName().equals(methodName)) {
            final Pair newPair = new Pair(factory.createCodeBlock(), blockCounter++);

            List<PsiStatement> statements1 = new ArrayList<>();
            statements1.add(factory.createStatementFromText("context.section = " + newPair.getId() + ";", null));
            final String s = Arrays.stream(expression.getArgumentList().getExpressions())
                    .map(PsiElement::getText).collect(Collectors.joining(","));
            statements1.add(factory.createStatementFromText("stack.push(new " +
                    contextClassName + "(" + s + "));", null));
            statements1.add(factory.createStatementFromText("break;", null));
            for (PsiStatement statement : statements1) {
                currentPair.getBlock().add(statement);
            }
            currentStatement.delete();

            this.currentPair = newPair;
            blocks.add(this.currentPair);

            final PsiElement parent = expression.getParent();
            if (parent instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) parent;
                currentPair.getBlock().add(factory.createStatementFromText(
                        assignment.getLExpression().getText() + " = ret;", null));
            }
        }

    }

    List<Pair> getBlocks() {
        return blocks;
    }
}
