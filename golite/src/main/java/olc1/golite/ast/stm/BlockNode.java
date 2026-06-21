package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class BlockNode implements ASTNode {
    private final ASTNode body;

    public BlockNode(ASTNode body) {
        this.body = body;
    }

    public static class Context {
        public final ASTNode body;

        public Context(BlockNode node) {
            this.body = node.body;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}