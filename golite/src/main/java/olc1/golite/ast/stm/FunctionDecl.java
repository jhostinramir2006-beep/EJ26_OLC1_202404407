package olc1.golite.ast.stm;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class FunctionDecl implements ASTNode {
    public final String name;
    public final List<ASTNode> params;
    public final String returnType;
    public final ASTNode body;

    public FunctionDecl(String name, List<ASTNode> params, String returnType, ASTNode body) {
        this.name = name;
        this.params = params;
        this.returnType = returnType;
        this.body = body;
    }

    public static class Context {
        public final String name;
        public final List<ASTNode> params;
        public final String returnType;
        public final ASTNode body;

        public Context(FunctionDecl node) {
            this.name = node.name;
            this.params = node.params;
            this.returnType = node.returnType;
            this.body = node.body;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}