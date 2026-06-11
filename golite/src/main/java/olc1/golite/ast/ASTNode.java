package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// MODIFICADO: Se agrego soporte para else y else if
// Antes: solo tenia condition y body
// Ahora: tiene condition, body y elseBody (puede ser null, otro IfNode, o un Statments)
//
// Ejemplos:
//   if x > 0 { ... }                          →  elseBody = null
//   if x > 0 { ... } else { ... }             →  elseBody = Statments
//   if x > 0 { ... } else if x < 0 { ... }    →  elseBody = IfNode
public class IfNode implements ASTNode {
    private final ASTNode condition;
    private final ASTNode body;
    private final ASTNode elseBody;  // AGREGADO: null, Statments, o IfNode

    public IfNode(ASTNode condition, ASTNode body, ASTNode elseBody) {
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
    }

    public static class Context {
        public final ASTNode condition;
        public final ASTNode body;
        public final ASTNode elseBody;  // null si no hay else

        public Context(IfNode node) {
            this.condition = node.condition;
            this.body = node.body;
            this.elseBody = node.elseBody;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}