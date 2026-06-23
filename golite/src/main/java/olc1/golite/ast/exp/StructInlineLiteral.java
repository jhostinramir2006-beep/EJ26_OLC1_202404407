package olc1.golite.ast.exp;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.ast.stm.StructAssignment;
import olc1.golite.visitor.Visitor;

public class StructInlineLiteral implements ASTNode {
    public final List<StructAssignment> values;

    public StructInlineLiteral(List<StructAssignment> values) {
        this.values = values;
    }

    public static class Context {
        public final List<StructAssignment> values;

        public Context(StructInlineLiteral node) {
            this.values = node.values;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}