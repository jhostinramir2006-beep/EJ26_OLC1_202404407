package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// AGREGADO: Nodo para el operador de incremento (++)
// Equivale a: variable = variable + 1
// Ejemplo: i++   →  i = i + 1
// En GoLite i++ es una sentencia, NO una expresion
public class Increment implements ASTNode {
    private final String name;
    private final int line;
    private final int column;

    public Increment(String name, int line, int column) {
        this.name = name;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final int line;
        public final int column;

        public Context(Increment node) {
            this.name = node.name;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}