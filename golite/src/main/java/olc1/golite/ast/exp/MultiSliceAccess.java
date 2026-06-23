package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class MultiSliceAccess implements ASTNode {
    public final String name;
    public final ASTNode row;
    public final ASTNode column;

    public MultiSliceAccess(String name, ASTNode row, ASTNode column) {
        this.name = name;
        this.row = row;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final ASTNode row;
        public final ASTNode column;

        public Context(MultiSliceAccess node) {
            this.name = node.name;
            this.row = node.row;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}