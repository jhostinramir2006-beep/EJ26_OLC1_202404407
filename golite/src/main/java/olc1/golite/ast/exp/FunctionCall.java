package olc1.golite.ast.exp;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class FunctionCall implements ASTNode {
    public final String name;
    public final List<ASTNode> args;

    public FunctionCall(String name, List<ASTNode> args) {
        this.name = name;
        this.args = args;
    }

    public static class Context {
        public final String name;
        public final List<ASTNode> args;

        public Context(FunctionCall node) {
            this.name = node.name;
            this.args = node.args;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}