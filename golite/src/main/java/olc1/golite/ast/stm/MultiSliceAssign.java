package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class MultiSliceAssign implements ASTNode {
    public final String name;
    public final ASTNode row;
    public final ASTNode column;
    public final ASTNode value;

    public MultiSliceAssign(String name, ASTNode row, ASTNode column, ASTNode value) {
        this.name = name;
        this.row = row;
        this.column = column;
        this.value = value;
    }

    public static class Context {
        public final String name;
        public final ASTNode row;
        public final ASTNode column;
        public final ASTNode value;

        public Context(MultiSliceAssign node) {
            this.name = node.name;
            this.row = node.row;
            this.column = node.column;
            this.value = node.value;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}