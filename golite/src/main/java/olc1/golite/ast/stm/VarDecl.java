package olc1.golite.ast.stm;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// AGREGADO: Nodo para declaracion de variable con tipo explicito
// Ejemplo: var x int = 5
//          var nombre string = "Hola"
//          var flag bool          (sin valor, toma el default)
public class VarDecl implements ASTNode {
    private final String name;    // nombre de la variable
    private final String type;    // tipo: "int", "float64", "string", "bool", "rune"
    private final ASTNode value;  // valor inicial (puede ser null si no se asigna)
    private final int line;
    private final int column;

    public VarDecl(String name, String type, ASTNode value, int line, int column) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final String type;
        public final ASTNode value;  // null si no hay valor inicial
        public final int line;
        public final int column;

        public Context(VarDecl node) {
            this.name = node.name;
            this.type = node.type;
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