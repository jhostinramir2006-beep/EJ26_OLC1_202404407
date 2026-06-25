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
        StringBuilder sb = new StringBuilder();

        sb.append(structName).append("{");

        int i = 0;

        for (Map.Entry<String, ValueWrapper> entry : attributes.entrySet()) {
            if (i > 0) sb.append(", ");

            sb.append(entry.getKey())
            .append(": ")
            .append(entry.getValue().toString());

            i++;
        }

        sb.append("}");

        return sb.toString();
    }
}