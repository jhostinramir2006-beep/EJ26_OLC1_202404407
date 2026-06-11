package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// AGREGADO: Nodo para la sentencia for en sus variantes
//
// Variante 1 - Como while (init=null, update=null):
//   for condicion { }
//   for i <= 5 { i++ }
//
// Variante 2 - For clasico (todos los campos presentes):
//   for i := 0; i < 10; i++ { }
//
// Nota: El for con range (Fase 2) no se incluye aqui
public class ForNode implements ASTNode {
    private final ASTNode init;       // puede ser null en variante while
    private final ASTNode condition;  // condicion de continuacion
    private final ASTNode update;     // puede ser null en variante while
    private final ASTNode body;       // bloque de instrucciones

    public ForNode(ASTNode init, ASTNode condition, ASTNode update, ASTNode body) {
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    public static class Context {
        public final ASTNode init;       // null si es variante while
        public final ASTNode condition;
        public final ASTNode update;     // null si es variante while
        public final ASTNode body;

        public Context(ForNode node) {
            this.init = node.init;
            this.condition = node.condition;
            this.update = node.update;
            this.body = node.body;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}