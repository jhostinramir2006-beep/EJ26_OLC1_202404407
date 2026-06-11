package olc1.golite.ast.exp;

import olc1.golite.ast.ASTNode;
import olc1.golite.visitor.Visitor;

// AGREGADO: Nodo para literales de tipo rune ('A', 'z', '\n')
// Ejemplo: var c rune = 'A'
public class RuneLiteral implements ASTNode {
    private final String value;  // El texto completo incluyendo comillas: 'A'
    private final int line;
    private final int column;

    public RuneLiteral(String value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String value;   // texto con comillas: 'A'
        public final char charValue; // el caracter real: A
        public final int line;
        public final int column;

        public Context(RuneLiteral node) {
            this.value = node.value;
            this.line = node.line;
            this.column = node.column;
            // Extraer el caracter real quitando las comillas simples
            String inner = node.value.substring(1, node.value.length() - 1);
            if (inner.startsWith("\\")) {
                // Secuencias de escape
                this.charValue = switch (inner) {
                    case "\\n"  -> '\n';
                    case "\\t"  -> '\t';
                    case "\\r"  -> '\r';
                    case "\\\\" -> '\\';
                    case "\\'"  -> '\'';
                    default     -> inner.charAt(1);
                };
            } else {
                this.charValue = inner.charAt(0);
            }
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}