package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class CaseNode implements ASTNode {
    private final ASTNode value;
    private final ASTNode body;

    public CaseNode(ASTNode value, ASTNode body) {
        this.value = value;
        this.body = body;
    }

    public static class Context {
        public final ASTNode value;
        public final ASTNode body;

        public Context(CaseNode node) {
            this.value = node.value;
            this.body = node.body;
        }
    }
    public ASTNode getValue() {
        return value;
    }

    public ASTNode getBody() {
        return body;
    }
    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}