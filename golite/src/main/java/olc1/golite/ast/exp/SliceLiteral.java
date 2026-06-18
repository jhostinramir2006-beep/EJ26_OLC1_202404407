package olc1.golite.ast.exp;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class SliceLiteral implements ASTNode {
    private final String type;
    private final List<ASTNode> values;

    public SliceLiteral(String type, List<ASTNode> values) {
        this.type = type;
        this.values = values;
    }

    public static class Context {
        public final String type;
        public final List<ASTNode> values;

        public Context(SliceLiteral node) {
            this.type = node.type;
            this.values = node.values;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}