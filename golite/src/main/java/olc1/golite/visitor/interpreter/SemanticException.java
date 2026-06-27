package olc1.golite.visitor.interpreter;

public class SemanticException extends RuntimeException {
    public final int line;
    public final int column;

    public SemanticException(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
    }
}