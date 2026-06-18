package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class ForRangeNode implements ASTNode {
    private final String indexName;
    private final String valueName;
    private final String sliceName;
    private final ASTNode body;
    private final int line;
    private final int column;

    public ForRangeNode(String indexName, String valueName, String sliceName, ASTNode body, int line, int column) {
        this.indexName = indexName;
        this.valueName = valueName;
        this.sliceName = sliceName;
        this.body = body;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String indexName;
        public final String valueName;
        public final String sliceName;
        public final ASTNode body;
        public final int line;
        public final int column;

        public Context(ForRangeNode node) {
            this.indexName = node.indexName;
            this.valueName = node.valueName;
            this.sliceName = node.sliceName;
            this.body = node.body;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}