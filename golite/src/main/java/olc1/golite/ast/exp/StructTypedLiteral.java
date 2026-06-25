package olc1.golite.ast.exp;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.ast.stm.StructAssignment;
import olc1.golite.visitor.Visitor;

public class StructTypedLiteral implements ASTNode {
    public final String structType;
    public final List<StructAssignment> values;

    public StructTypedLiteral(String structType, List<StructAssignment> values) {
        this.structType = structType;
        this.values = values;
    }

    public static class Context {
        public final String structType;
        public final List<StructAssignment> values;

        public Context(StructTypedLiteral node) {
            this.structType = node.structType;
            this.values = node.values;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}