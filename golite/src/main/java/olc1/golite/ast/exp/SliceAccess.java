package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class SliceAccess implements ASTNode {
    public final String name;
    public final ASTNode index;

    public SliceAccess(String name, ASTNode index) {
        this.name = name;
        this.index = index;
    }

    public static class Context {
        public final String name;
        public final ASTNode index;

        public Context(SliceAccess node) {
            this.name = node.name;
            this.index = node.index;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}