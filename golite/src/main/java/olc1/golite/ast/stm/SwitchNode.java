package olc1.golite.ast.stm;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class SwitchNode implements ASTNode {
    private final ASTNode expression;
    private final List<ASTNode> cases;
    private final ASTNode defaultBody;

    public SwitchNode(ASTNode expression, List<ASTNode> cases, ASTNode defaultBody) {
        this.expression = expression;
        this.cases = cases;
        this.defaultBody = defaultBody;
    }

    public static class Context {
        public final ASTNode expression;
        public final List<ASTNode> cases;
        public final ASTNode defaultBody;

        public Context(SwitchNode node) {
            this.expression = node.expression;
            this.cases = node.cases;
            this.defaultBody = node.defaultBody;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}