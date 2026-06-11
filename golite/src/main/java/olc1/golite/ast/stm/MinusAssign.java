package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// AGREGADO: Nodo para el operador de asignacion compuesta -=
// Equivale a: variable = variable - expresion
// Ejemplo: x -= 5   →  x = x - 5
public class MinusAssign implements ASTNode {
    private final String name;
    private final ASTNode value;
    private final int line;
    private final int column;

    public MinusAssign(String name, ASTNode value, int line, int column) {
        this.name = name;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final ASTNode value;
        public final int line;
        public final int column;

        public Context(MinusAssign node) {
            this.name = node.name;
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