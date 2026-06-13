package olc1.golite.ast.stm;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class FmtPrintln implements ASTNode {
    private final List<ASTNode> arguments;

    public FmtPrintln(List<ASTNode> arguments) {
        this.arguments = arguments;
    }

    public static class Context {
        public final List<ASTNode> arguments;

        public Context(FmtPrintln node) {
            this.arguments = node.arguments;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}