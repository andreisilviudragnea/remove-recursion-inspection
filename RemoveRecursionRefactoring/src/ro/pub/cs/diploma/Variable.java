package ro.pub.cs.diploma;

import com.intellij.psi.PsiType;

public class Variable {
    private final String name;
    private final PsiType type;

    public Variable(String name, PsiType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public PsiType getType() {
        return type;
    }
}
