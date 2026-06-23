package olc1.golite.visitor.interpreter.value;

import java.util.Map;

public record StructValue(
        String structName,
        Map<String, ValueWrapper> attributes,
        int line,
        int column
) implements ValueWrapper {

    @Override
    public String getTypeName() {
        return structName;
    }

    @Override
    public String toString() {
        return structName + attributes.toString();
    }
}