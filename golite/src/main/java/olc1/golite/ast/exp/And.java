package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class And implements ASTNode {
    private final ASTNode left;
    private final ASTNode right;
    private final int line;
    private final int column;

    public And(ASTNode left, ASTNode right, int line, int column) {
        this.left = left;
        this.right = right;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode left;
        public final ASTNode right;
        public final int line;
        public final int column;

        public Context(And node) {
            this.left = node.left;
            this.right = node.right;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}