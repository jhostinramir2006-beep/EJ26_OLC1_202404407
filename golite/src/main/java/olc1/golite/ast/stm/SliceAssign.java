package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class SliceAssign implements ASTNode {
    public final String name;
    public final ASTNode index;
    public final ASTNode value;

    public SliceAssign(String name, ASTNode index, ASTNode value) {
        this.name = name;
        this.index = index;
        this.value = value;
    }

    public static class Context {
        public final String name;
        public final ASTNode index;
        public final ASTNode value;

        public Context(SliceAssign node) {
            this.name = node.name;
            this.index = node.index;
            this.value = node.value;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}