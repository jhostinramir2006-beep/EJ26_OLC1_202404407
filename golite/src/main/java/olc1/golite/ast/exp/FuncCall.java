package olc1.golite.ast.exp;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// AGREGADO: Nodo para llamadas a funciones usadas como expresion
// Ejemplo: sumar(a, b), obtenerNumero()
public class FuncCall implements ASTNode {
    private final String name;
    private final List<ASTNode> arguments;
    private final int line;
    private final int column;

    public FuncCall(String name, List<ASTNode> arguments, int line, int column) {
        this.name = name;
        this.arguments = arguments;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final List<ASTNode> arguments;
        public final int line;
        public final int column;

        public Context(FuncCall node) {
            this.name = node.name;
            this.arguments = node.arguments;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}