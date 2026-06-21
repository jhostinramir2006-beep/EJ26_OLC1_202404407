package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class ParameterNode implements ASTNode {
    public final String name;
    public final String type;

    public ParameterNode(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public static class Context {
        public final String name;
        public final String type;

        public Context(ParameterNode node) {
            this.name = node.name;
            this.type = node.type;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}