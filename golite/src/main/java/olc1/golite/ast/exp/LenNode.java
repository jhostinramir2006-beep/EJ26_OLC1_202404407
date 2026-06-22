package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class LenNode implements ASTNode {

    private final ASTNode expression;

    public LenNode(ASTNode expression) {
        this.expression = expression;
    }

    public static class Context {
        public final ASTNode expression;

        public Context(LenNode node) {
            this.expression = node.expression;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}