package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class StructAssign implements ASTNode {
    public final String varName;
    public final String fieldName;
    public final ASTNode value;

    public StructAssign(String varName, String fieldName, ASTNode value) {
        this.varName = varName;
        this.fieldName = fieldName;
        this.value = value;
    }

    public static class Context {
        public final String varName;
        public final String fieldName;
        public final ASTNode value;

        public Context(StructAssign node) {
            this.varName = node.varName;
            this.fieldName = node.fieldName;
            this.value = node.value;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}