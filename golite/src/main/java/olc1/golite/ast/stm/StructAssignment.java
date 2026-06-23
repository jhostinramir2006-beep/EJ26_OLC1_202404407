package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;

public class StructAssignment {
    public final String name;
    public final ASTNode value;

    public StructAssignment(String name, ASTNode value) {
        this.name = name;
        this.value = value;
    }
}