package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class ForNode implements ASTNode {
    private final ASTNode init;
    private final ASTNode condition;
    private final ASTNode update;
    private final ASTNode body;

    public ForNode(ASTNode init, ASTNode condition, ASTNode update, ASTNode body) {
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    public static class Context {
        public final ASTNode init;
        public final ASTNode condition;
        public final ASTNode update;
        public final ASTNode body;

        public Context(ForNode node) {
            this.init = node.init;
            this.condition = node.condition;
            this.update = node.update;
            this.body = node.body;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}