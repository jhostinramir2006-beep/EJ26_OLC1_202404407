package olc1.golite.visitor.interpreter.value;

import java.util.List;

public record SliceValue(String elementType, List<ValueWrapper> values, int line, int column)
        implements ValueWrapper {

    @Override
    public String getTypeName() {
        return "[]" + elementType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(values.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }
}