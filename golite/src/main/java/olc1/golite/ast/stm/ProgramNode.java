package olc1.golite.ast.stm;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class ProgramNode implements ASTNode {
    public final List<ASTNode> functions;

    public ProgramNode(List<ASTNode> functions) {
        this.functions = functions;
    }

    public static class Context {
        public final List<ASTNode> functions;

        public Context(ProgramNode node) {
            this.functions = node.functions;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}