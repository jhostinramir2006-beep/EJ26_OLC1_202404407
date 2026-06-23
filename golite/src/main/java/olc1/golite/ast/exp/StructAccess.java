package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class StructAccess implements ASTNode {
    public final String varName;
    public final String fieldName;

    public StructAccess(String varName, String fieldName) {
        this.varName = varName;
        this.fieldName = fieldName;
    }

    public static class Context {
        public final String varName;
        public final String fieldName;

        public Context(StructAccess node) {
            this.varName = node.varName;
            this.fieldName = node.fieldName;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}