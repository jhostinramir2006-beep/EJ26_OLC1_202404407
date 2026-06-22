package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class SlicesIndexNode implements ASTNode {

    private final ASTNode slice;
    private final ASTNode value;

    public SlicesIndexNode(ASTNode slice, ASTNode value) {
        this.slice = slice;
        this.value = value;
    }

    public static class Context {
        public final ASTNode slice;
        public final ASTNode value;

        public Context(SlicesIndexNode node) {
            this.slice = node.slice;
            this.value = node.value;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}