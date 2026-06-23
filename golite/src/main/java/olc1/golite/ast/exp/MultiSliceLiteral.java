package olc1.golite.ast.exp;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class MultiSliceLiteral implements ASTNode {
    public final String type;
    public final List<ASTNode> rows;

    public MultiSliceLiteral(String type, List<ASTNode> rows) {
        this.type = type;
        this.rows = rows;
    }

    public static class Context {
        public final String type;
        public final List<ASTNode> rows;

        public Context(MultiSliceLiteral node) {
            this.type = node.type;
            this.rows = node.rows;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}