package olc1.golite.ast.stm;

import java.util.List;
import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// AGREGADO: Nodo para la funcion embebida fmt.Println(...)
// Imprime uno o mas valores separados por espacio con salto de linea al final
// Ejemplos:
//   fmt.Println("Hola GoLite")
//   fmt.Println("Valor:", x)
//   fmt.Println(10, true, 'A')
//   fmt.Println()              →  solo imprime salto de linea
public class FmtPrintln implements ASTNode {
    private final List<ASTNode> arguments;
    private final int line;
    private final int column;

    public FmtPrintln(List<ASTNode> arguments, int line, int column) {
        this.arguments = arguments;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final List<ASTNode> arguments;
        public final int line;
        public final int column;

        public Context(FmtPrintln node) {
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