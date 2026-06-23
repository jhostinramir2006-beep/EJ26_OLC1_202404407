package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class StructAccessExpr implements ASTNode {
    public final ASTNode object;
    public final String fieldName;

    public StructAccessExpr(ASTNode object, String fieldName) {
        this.object = object;
        this.fieldName = fieldName;
    }

    public static class Context {
        public final ASTNode object;
        public final String fieldName;

        public Context(StructAccessExpr node) {
            this.object = node.object;
            this.fieldName = node.fieldName;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}