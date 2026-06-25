package olc1.golite.ast.exp;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class StructMethodCall implements ASTNode {
    public final ASTNode object;
    public final String methodName;
    public final List<ASTNode> args;

    public StructMethodCall(ASTNode object, String methodName, List<ASTNode> args) {
        this.object = object;
        this.methodName = methodName;
        this.args = args;
    }

    public static class Context {
        public final ASTNode object;
        public final String methodName;
        public final List<ASTNode> args;

        public Context(StructMethodCall node) {
            this.object = node.object;
            this.methodName = node.methodName;
            this.args = node.args;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}