package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

public class NilLiteral implements ASTNode {
    public static class Context {
        public Context(NilLiteral node) {}
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}