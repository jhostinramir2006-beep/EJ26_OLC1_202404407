package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class ReturnNode implements ASTNode {
    public final ASTNode value;

    public ReturnNode(ASTNode value) {
        this.value = value;
    }

    public static class Context {
        public final ASTNode value;

        public Context(ReturnNode node) {
            this.value = node.value;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}