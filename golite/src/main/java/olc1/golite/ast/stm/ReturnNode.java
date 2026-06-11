package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// AGREGADO: Nodo para la sentencia return
// Puede retornar un valor o nada (void)
// Ejemplo: return a + b
//          return        (sin valor)
public class ReturnNode implements ASTNode {
    private final ASTNode value;  // puede ser null si no retorna nada
    private final int line;
    private final int column;

    public ReturnNode(ASTNode value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode value;  // null si return sin valor
        public final int line;
        public final int column;

        public Context(ReturnNode node) {
            this.value = node.value;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}