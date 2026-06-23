package olc1.golite.ast.stm;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class StructVarDecl implements ASTNode {
    public final String structType;
    public final String varName;
    public final List<StructAssignment> values;

    public StructVarDecl(String structType, String varName, List<StructAssignment> values) {
        this.structType = structType;
        this.varName = varName;
        this.values = values;
    }

    public static class Context {
        public final String structType;
        public final String varName;
        public final List<StructAssignment> values;

        public Context(StructVarDecl node) {
            this.structType = node.structType;
            this.varName = node.varName;
            this.values = node.values;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}