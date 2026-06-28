package olc1.golite.reports;

import olc1.golite.ast.ASTNode;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class AstGraphGenerator {

    private final StringBuilder dot = new StringBuilder();
    private int counter = 0;

    public String generate(ASTNode root) {
        dot.setLength(0);
        counter = 0;

        dot.append("digraph AST {\n");
        dot.append("rankdir=TB;\n");
        dot.append("node [shape=box, style=filled, fillcolor=\"#DCEBFF\", fontname=\"Arial\"];\n");
        dot.append("edge [color=\"#444444\"];\n");

        int rootId = createNode("program");
        int childId = visit(root, root.getClass().getSimpleName());
        addEdge(rootId, childId);

        dot.append("}\n");
        return dot.toString();
    }

    private int visit(Object obj, String label) {
        if (obj == null) {
            return createNode("null");
        }

        if (!(obj instanceof ASTNode)) {
            return createNode(label + ": " + obj);
        }

        int nodeId = createNode(label);

        for (Field field : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            field.setAccessible(true);

            try {
                Object value = field.get(obj);

                if (value == null) continue;

                if (value instanceof ASTNode child) {
                    int childId = visit(child, field.getName());
                    addEdge(nodeId, childId);

                } else if (value instanceof List<?> list) {
                    int listId = createNode(field.getName());
                    addEdge(nodeId, listId);

                    for (Object item : list) {
                        if (item == null) continue;

                        if (item instanceof ASTNode astItem) {
                            int childId = visit(astItem, item.getClass().getSimpleName());
                            addEdge(listId, childId);
                        } else {
                            int childId = createNode(String.valueOf(item));
                            addEdge(listId, childId);
                        }
                    }

                } else if (isSimple(value)) {
                    int childId = createNode(field.getName() + ": " + value);
                    addEdge(nodeId, childId);
                }

            } catch (Exception ignored) {
            }
        }

        return nodeId;
    }

    private int createNode(String label) {
        int id = counter++;

        dot.append("node")
           .append(id)
           .append(" [label=\"")
           .append(escape(label))
           .append("\"];\n");

        return id;
    }

    private void addEdge(int from, int to) {
        dot.append("node")
           .append(from)
           .append(" -> node")
           .append(to)
           .append(";\n");
    }

    private boolean isSimple(Object value) {
        return value instanceof String ||
               value instanceof Number ||
               value instanceof Boolean ||
               value instanceof Character;
    }

    private String escape(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}