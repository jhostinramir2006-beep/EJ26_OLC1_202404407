package olc1.golite.ast.stm;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class StructMethodDecl implements ASTNode {
    public final String referenceName;
    public final String structName;
    public final String methodName;
    public final List<ASTNode> params;
    public final String returnType;
    public final ASTNode body;

    public StructMethodDecl(String referenceName, String structName, String methodName,
                            List<ASTNode> params, String returnType, ASTNode body) {
        this.referenceName = referenceName;
        this.structName = structName;
        this.methodName = methodName;
        this.params = params;
        this.returnType = returnType;
        this.body = body;
    }

    public static class Context {
        public final String referenceName;
        public final String structName;
        public final String methodName;
        public final List<ASTNode> params;
        public final String returnType;
        public final ASTNode body;

        public Context(StructMethodDecl node) {
            this.referenceName = node.referenceName;
            this.structName = node.structName;
            this.methodName = node.methodName;
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