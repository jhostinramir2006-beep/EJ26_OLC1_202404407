package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class StringsJoinNode implements ASTNode {

    private final ASTNode slice;
    private final ASTNode separator;

    public StringsJoinNode(ASTNode slice, ASTNode separator) {
        this.slice = slice;
        this.separator = separator;
    }

    public static class Context {
        public final ASTNode slice;
        public final ASTNode separator;

        public Context(StringsJoinNode node) {
            this.slice = node.slice;
            this.separator = node.separator;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}