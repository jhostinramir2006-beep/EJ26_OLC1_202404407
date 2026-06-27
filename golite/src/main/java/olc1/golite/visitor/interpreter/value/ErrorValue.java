package olc1.golite.visitor.interpreter.value;

public record ErrorValue(int line, int column) implements ValueWrapper {

    @Override
    public String getTypeName() {
        return "error";
    }

    @Override
    public String toString() {
        return "";
    }
}