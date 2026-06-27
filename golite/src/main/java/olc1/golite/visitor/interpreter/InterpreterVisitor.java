package olc1.golite.visitor.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import olc1.golite.ast.ASTNode;
import olc1.golite.ast.exp.*;
import olc1.golite.ast.stm.*;
import olc1.golite.reports.GoliteError;
import olc1.golite.visitor.Visitor;
import olc1.golite.visitor.interpreter.value.*;

public class InterpreterVisitor implements Visitor<ValueWrapper> {
    public String output = "";
    private String expectedStructType = null;
    private final ValueWrapper defaultVoid = new VoidValue(-1, -1);
    private final Map<String, ValueWrapper> variables = new HashMap<>();   
    private boolean insideMethod = false;
    private final Map<String, StructDecl.Context> structDefs = new HashMap<>(); 
    private final Map<String, StructMethodDecl.Context> structMethods = new HashMap<>();
    public final List<GoliteError> semanticErrors = new ArrayList<>();
    private final java.util.Deque<Map<String, ValueWrapper>> scopes = new java.util.ArrayDeque<>();
    private final Map<String, FunctionDecl.Context> functions = new HashMap<>();

    public InterpreterVisitor() {
        scopes.push(new HashMap<>());
    }
    private Map<String, ValueWrapper> currentScope() {
    return scopes.peek();
    }

    private ValueWrapper getVariable(String name) {
        for (Map<String, ValueWrapper> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }
    private String methodKey(String structName, String methodName) {
        return structName + "." + methodName;
    }
    private ValueWrapper defaultValue(String type) {
        return switch (type) {
            case "int" -> new IntValue(0, -1, -1);
            case "float64" -> new DecimalValue(0.0, -1, -1);
            case "string" -> new StringValue("", -1, -1);
            case "bool" -> new BoolValue(false, -1, -1);
            case "rune" -> new RuneValue('\0', -1, -1);
            default -> {
                StructDecl.Context def = structDefs.get(type);
                if (def != null) {
                    Map<String, ValueWrapper> attrs = new HashMap<>();
                    for (StructField field : def.fields) {
                        attrs.put(field.name, defaultValue(field.type));
                    }
                    yield new StructValue(type, attrs, -1, -1);
                }

                yield new VoidValue(-1, -1);
            }
        };
    }
    private void addSemanticError(String message, int line, int column) {
        semanticErrors.add(new GoliteError("Semantico", message, line, column));
    }
    private void ensureAssignable(String expected, ValueWrapper value) {
        String actual = value.getTypeName();

        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "No se puede asignar " + actual + " a " + expected
            );
        }
    }
    private void ensureBool(ValueWrapper value, String context) {
        if (!value.getTypeName().equals("bool")) {
            throw new RuntimeException(
                context + " debe ser bool, se obtuvo " + value.getTypeName()
            );
        }
    }
    private void ensureInt(ValueWrapper value, String context) {
        if (!value.getTypeName().equals("int")) {
            throw new RuntimeException(
                context + " debe ser int, se obtuvo " + value.getTypeName()
            );
        }
    }
    private void ensureSameType(ValueWrapper a, ValueWrapper b, String op) {
        if (!a.getTypeName().equals(b.getTypeName())) {
            throw new RuntimeException(
                "Tipos incompatibles " + a.getTypeName() + " " + op + " " + b.getTypeName()
            );
        }
    }
    private ValueWrapper copyIfPrimitive(ValueWrapper value) {
        if (value instanceof IntValue v) {
            return new IntValue(v.value(), v.line(), v.column());
        }

        if (value instanceof DecimalValue v) {
            return new DecimalValue(v.value(), v.line(), v.column());
        }

        if (value instanceof StringValue v) {
            return new StringValue(v.value(), v.line(), v.column());
        }

        if (value instanceof BoolValue v) {
            return new BoolValue(v.value(), v.line(), v.column());
        }

        if (value instanceof RuneValue v) {
            return new RuneValue(v.value(), v.line(), v.column());
        }

        return value; // structs y slices por referencia
    }
    private boolean equalsValues(ValueWrapper a, ValueWrapper b) {
        if (a instanceof IntValue x && b instanceof IntValue y) return x.value() == y.value();
        if (a instanceof DecimalValue x && b instanceof DecimalValue y) return x.value() == y.value();
        if (a instanceof IntValue x && b instanceof DecimalValue y) return x.value() == y.value();
        if (a instanceof DecimalValue x && b instanceof IntValue y) return x.value() == y.value();
        if (a instanceof StringValue x && b instanceof StringValue y) return x.value().equals(y.value());
        if (a instanceof BoolValue x && b instanceof BoolValue y) return x.value() == y.value();
        if (a instanceof RuneValue x && b instanceof RuneValue y) return x.value() == y.value();
        return false;
    }
    private void setVariable(String name, ValueWrapper value) {
        for (Map<String, ValueWrapper> scope : scopes) {
            if (scope.containsKey(name)) {
                scope.put(name, value);
                return;
            }
        }

        currentScope().put(name, value);
    }
    public ValueWrapper Visit(ASTNode node) {
        if (node == null) {
            return defaultVoid;
        }

        return node.accept(this);
    }

    @Override
    public ValueWrapper visit(Integers.Context ctx) {
        return new IntValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(Decimal.Context ctx) {
        return new DecimalValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(Add.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue(l.value() + r.value(), l.line(), l.column());

            case IntValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() + r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() + r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() + r.value(), l.line(), l.column());

            case StringValue l when right instanceof StringValue r -> {
                String a = l.value();
                String b = r.value();

                a = a.replace("\"", "");
                b = b.replace("\"", "");

                yield new StringValue(a + b, l.line(), l.column());
            }

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " + " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(Sub.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue((int)(l.value() - r.value()), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue(l.value() - r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() - r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() - r.value(), l.line(), l.column());
            default -> throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " - " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Mul.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() * r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue(l.value() * r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() * r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() * r.value(), l.line(), l.column());
            default -> throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " * " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Div.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        if (left instanceof ErrorValue || right instanceof ErrorValue) {
            return new ErrorValue(left.line(), left.column());
        }

        if ((right instanceof IntValue r1 && r1.value() == 0) ||
            (right instanceof DecimalValue r2 && r2.value() == 0.0)) {
            throw new SemanticException(
                "Division entre cero",
                left.line(),
                left.column()
            );
        }

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue(l.value() / r.value(), left.line(), left.column());

            case IntValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() / r.value(),left.line(), left.column());

            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() / r.value(),left.line(), left.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() / r.value(), left.line(), left.column());

            default -> throw new SemanticException(
                "Operacion invalida: " + left.getTypeName() + " / " + right.getTypeName(),
                left.line(),
                left.column()
            );
        };
    }

    @Override
    public ValueWrapper visit(Negate.Context ctx) {
        ValueWrapper operand = Visit(ctx.expression);
        return switch (operand) {
            case IntValue     v -> new IntValue(-v.value(), v.line(), v.column());
            case DecimalValue v -> new DecimalValue(-v.value(), v.line(), v.column());
            default -> throw new RuntimeException("Operacion invalida: -" + operand.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Imprimir.Context ctx) {
        ValueWrapper value = Visit(ctx.expression);
        output += value.toString() + "\n";
        return defaultVoid;
    }

@Override
public ValueWrapper visit(Statments.Context ctx) {
    for (ASTNode statement : ctx.statements) {
        if (statement == null) continue;

        try {
            Visit(statement);

        } catch (ReturnException e) {
            throw e;

        } catch (SemanticException e) {
            addSemanticError(e.getMessage(), e.line, e.column);

        } catch (BreakException e) {
            addSemanticError("Sentencia break fuera de un ciclo", -1, -1);

        } catch (ContinueException e) {
            addSemanticError("Sentencia continue fuera de un ciclo", -1, -1);

        } catch (Exception e) {
            if (e.getMessage() != null) {
                addSemanticError(e.getMessage(), -1, -1);
            }
        }
    }

    return defaultVoid;
}

    @Override
    public ValueWrapper visit(Paren.Context ctx) {
        return Visit(ctx.expression);
    }

    @Override
    public ValueWrapper visit(BoolLiteral.Context ctx) {
        return new BoolValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(StringLiteral.Context ctx) {
        String valor = ctx.value;

        valor = valor.substring(1, valor.length() - 1);

        return new StringValue(valor, ctx.line, ctx.column);
    }
    @Override
    public ValueWrapper visit(VarRef.Context ctx) {
        ValueWrapper val = getVariable(ctx.name);

        if (val == null) {
           throw new SemanticException(
                "Variable \"" + ctx.name + "\" no declarada",
                ctx.line,
                ctx.column
            );
        }

        return val;
    }

    @Override
    public ValueWrapper visit(Assign.Context ctx) {
        ValueWrapper old = getVariable(ctx.name);

        if (old == null) {
            throw new SemanticException(
                "Variable \"" + ctx.name + "\" no declarada",
                ctx.line,
                ctx.column
            );
        }

        ValueWrapper val = Visit(ctx.value);

        if (val instanceof ErrorValue) {
            setVariable(ctx.name, val);
            return defaultVoid;
        }

        if (old.getTypeName().equals("float64") && val.getTypeName().equals("int")) {
            IntValue i = (IntValue) val;
            val = new DecimalValue(i.value(), ctx.line, ctx.column);
        } else if (!old.getTypeName().equals(val.getTypeName())) {
            throw new SemanticException(
                "No se puede asignar " + val.getTypeName() + " a " + old.getTypeName(),
                ctx.line,
                ctx.column
            );
        }

        setVariable(ctx.name, val);
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(IfNode.Context ctx) {
        ValueWrapper cond = Visit(ctx.condition);

        if (!(cond instanceof BoolValue b)) {
            throw new RuntimeException("La condicion del if debe ser booleana");
        }

        if (b.value()) {
            Visit(ctx.body);
        } else if (ctx.elseBody != null) {
            Visit(ctx.elseBody);
        }

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(GreaterThan.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() > r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() > r.value(), l.line(), l.column());

            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() > r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() > r.value(), l.line(), l.column());

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " > " + right.getTypeName()
            );
        };
    }
    @Override
    public ValueWrapper visit(LessThan.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() < r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() < r.value(), l.line(), l.column());

            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() < r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() < r.value(), l.line(), l.column());

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " < " + right.getTypeName()
            );
        };
    }
    @Override
    public ValueWrapper visit(Equals.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        if (left instanceof ErrorValue || right instanceof ErrorValue) {
            return new ErrorValue(ctx.line, ctx.column);
        }

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() == r.value(), ctx.line, ctx.column);

            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() == r.value(), ctx.line, ctx.column);

            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() == r.value(), ctx.line, ctx.column);

            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() == r.value(), ctx.line, ctx.column);

            case BoolValue l when right instanceof BoolValue r ->
                new BoolValue(l.value() == r.value(), ctx.line, ctx.column);

            case StringValue l when right instanceof StringValue r ->
                new BoolValue(l.value().equals(r.value()), ctx.line, ctx.column);

            case RuneValue l when right instanceof RuneValue r ->
                new BoolValue(l.value() == r.value(), ctx.line, ctx.column);

            default -> throw new SemanticException(
                "Operacion invalida: " + left.getTypeName() + " == " + right.getTypeName(),
                ctx.line,
                ctx.column
            );
        };
    }
    @Override
    public ValueWrapper visit(NotEquals.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());

            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());

            case BoolValue l when right instanceof BoolValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());

            case StringValue l when right instanceof StringValue r ->
                new BoolValue(!l.value().equals(r.value()), l.line(), l.column());

            case RuneValue l when right instanceof RuneValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " != " + right.getTypeName()
            );
        };
    }
    @Override
    public ValueWrapper visit(GreaterEqual.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() >= r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() >= r.value(), l.line(), l.column());

            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() >= r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() >= r.value(), l.line(), l.column());

            case RuneValue l when right instanceof RuneValue r ->
                new BoolValue(l.value() >= r.value(), l.line(), l.column());

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " >= " + right.getTypeName()
            );
        };
    }
    @Override
    public ValueWrapper visit(LessEqual.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() <= r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() <= r.value(), l.line(), l.column());

            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() <= r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() <= r.value(), l.line(), l.column());
            
            case RuneValue l when right instanceof RuneValue r ->
                new BoolValue(l.value() <= r.value(), l.line(), l.column());

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " <= " + right.getTypeName()
            );
        };
    }
    @Override
    public ValueWrapper visit(And.Context ctx) {
        ValueWrapper left = Visit(ctx.left);

        if (left instanceof ErrorValue) {
            return new ErrorValue(ctx.line, ctx.column);
        }

        if (!(left instanceof BoolValue l)) {
            throw new SemanticException(
                "Operacion invalida: && requiere booleanos",
                ctx.line,
                ctx.column
            );
        }

        if (!l.value()) {
            return new BoolValue(false, ctx.line, ctx.column);
        }

        ValueWrapper right = Visit(ctx.right);

        if (right instanceof ErrorValue) {
            return new ErrorValue(ctx.line, ctx.column);
        }

        if (!(right instanceof BoolValue r)) {
            throw new SemanticException(
                "Operacion invalida: && requiere booleanos",
                ctx.line,
                ctx.column
            );
        }

        return new BoolValue(r.value(), ctx.line, ctx.column);
    }
    @Override
    public ValueWrapper visit(Or.Context ctx) {
        ValueWrapper left = Visit(ctx.left);

        if (!(left instanceof BoolValue l)) {
            throw new RuntimeException("Operacion invalida: || requiere booleanos");
        }

        if (l.value()) {
            return new BoolValue(true, l.line(), l.column());
        }

        ValueWrapper right = Visit(ctx.right);

        if (!(right instanceof BoolValue r)) {
            throw new RuntimeException("Operacion invalida: || requiere booleanos");
        }

        return new BoolValue(r.value(), r.line(), r.column());
    }
    @Override
    public ValueWrapper visit(Not.Context ctx) {
        ValueWrapper expression = Visit(ctx.expression);

        if (!(expression instanceof BoolValue e)) {
            throw new RuntimeException("Operacion invalida: ! requiere un booleano");
        }

        return new BoolValue(!e.value(), e.line(), e.column());
    }
    @Override
    public ValueWrapper visit(Mod.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        if (left instanceof IntValue l && right instanceof IntValue r) {
            if (r.value() == 0) {
                throw new RuntimeException("Modulo por 0");
            }
            return new IntValue(l.value() % r.value(), l.line(), l.column());
        }

        throw new RuntimeException("Operacion invalida: % solo acepta enteros");
    }
    @Override
    public ValueWrapper visit(PlusAssign.Context ctx) {
        ValueWrapper current = getVariable(ctx.name);

        if (current == null) {
            throw new SemanticException(
                "Variable \"" + ctx.name + "\" no declarada",
                ctx.line,
                ctx.column
            );
        }

        ValueWrapper right = Visit(ctx.value);

        if (right instanceof ErrorValue) {
            setVariable(ctx.name, right);
            return defaultVoid;
        }

        if (current.getTypeName().equals("int") && right.getTypeName().equals("float64")) {
            throw new SemanticException(
                "No se puede aplicar += float64 a int",
                ctx.line,
                ctx.column
            );
        }

        ValueWrapper result = switch (current) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue(l.value() + r.value(), ctx.line, ctx.column);

            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() + r.value(), ctx.line, ctx.column);

            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() + r.value(), ctx.line, ctx.column);

            case StringValue l when right instanceof StringValue r ->
                new StringValue(l.value() + r.value(), ctx.line, ctx.column);

            default -> throw new SemanticException(
                "Operacion invalida: " + current.getTypeName() + " += " + right.getTypeName(),
                ctx.line,
                ctx.column
            );
        };

        setVariable(ctx.name, result);
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(MinusAssign.Context ctx) {
        ValueWrapper current = getVariable(ctx.name);

        if (current == null) {
            throw new RuntimeException("Variable no definida: " + ctx.name);
        }

        ValueWrapper right = Visit(ctx.value);

        ValueWrapper result = switch (current) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue(l.value() - r.value(), l.line(), l.column());

            case IntValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() - r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() - r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() - r.value(), l.line(), l.column());

            default -> throw new RuntimeException(
                "Operacion invalida: " + current.getTypeName() + " -= " + right.getTypeName()
            );
        };

        setVariable(ctx.name, result);
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(ForNode.Context ctx) {
        if (ctx.init != null) {
            Visit(ctx.init);
        }

        while (true) {
            ValueWrapper cond = Visit(ctx.condition);

            if (!(cond instanceof BoolValue b)) {
                throw new RuntimeException("La condicion del for debe ser booleana");
            }

            if (!b.value()) {
                break;
            }

            try {
                Visit(ctx.body);
            } catch (ContinueException e) {
                if (ctx.update != null) {
                    Visit(ctx.update);
                }
                continue;
            } catch (BreakException e) {
                break;
            }

            if (ctx.update != null) {
                Visit(ctx.update);
            }
        }

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(Increment.Context ctx) {
        ValueWrapper val = getVariable(ctx.name);

        if (val == null) {
            throw new RuntimeException("Variable no definida: " + ctx.name);
        }

        if (val instanceof IntValue v) {
            setVariable(ctx.name, new IntValue(v.value() + 1, v.line(), v.column()));
            return defaultVoid;
        }

        if (val instanceof DecimalValue v) {
            setVariable(ctx.name, new DecimalValue(v.value() + 1, v.line(), v.column()));
            return defaultVoid;
        }

        throw new RuntimeException("Operacion invalida: " + ctx.name + "++");
    }
    @Override
    public ValueWrapper visit(Decrement.Context ctx) {
        ValueWrapper val = getVariable(ctx.name);

        if (val == null) {
            throw new RuntimeException("Variable no definida: " + ctx.name);
        }

        if (val instanceof IntValue v) {
            setVariable(ctx.name, new IntValue(v.value() - 1, v.line(), v.column()));
            return defaultVoid;
        }

        if (val instanceof DecimalValue v) {
            setVariable(ctx.name, new DecimalValue(v.value() - 1, v.line(), v.column()));
            return defaultVoid;
        }

        throw new RuntimeException("Operacion invalida: " + ctx.name + "--");
    }
    @Override
    public ValueWrapper visit(FmtPrintln.Context ctx) {
        StringBuilder linea = new StringBuilder();

        for (ASTNode arg : ctx.arguments) {
            try {
                ValueWrapper value = Visit(arg);

                if (value instanceof ErrorValue) {
                    continue;
                }

                if (linea.length() > 0) {
                    linea.append(" ");
                }

                linea.append(value.toString());

            } catch (SemanticException e) {
                addSemanticError(e.getMessage(), e.line, e.column);
                continue;

            } catch (Exception e) {
                if (e.getMessage() != null) {
                    addSemanticError(e.getMessage(), -1, -1);
                }
                continue;
            }
        }

        if (linea.length() > 0) {
            output += linea + "\n";
        }

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(BreakNode.Context ctx) {
        throw new SemanticException(
            "Sentencia break fuera de un ciclo",
            ctx.line,
            ctx.column
        );
    }

    @Override
    public ValueWrapper visit(ContinueNode.Context ctx) {
        throw new SemanticException(
            "Sentencia continue fuera de un ciclo",
            ctx.line,
            ctx.column
        );
    }
    @Override
    public ValueWrapper visit(VarDeclInfer.Context ctx) {
        if (ctx.name.equals("__error_id__")) {
            return defaultVoid;
        }
        ValueWrapper val = Visit(ctx.value);
        currentScope().put(ctx.name, val);
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(VarDecl.Context ctx) {

        if (ctx.name.equals("__error_id__")) {
            return defaultVoid;
        }

        if (currentScope().containsKey(ctx.name)) {
            throw new SemanticException(
                "Variable \"" + ctx.name + "\" ya declarada",
                ctx.line,
                ctx.column
            );
        }

        ValueWrapper val;

        if (ctx.value != null) {
            try {
                val = Visit(ctx.value);

                if (val instanceof ErrorValue) {
                    currentScope().put(ctx.name, val);
                    return defaultVoid;
                }

                if (ctx.type.equals("float64") && val.getTypeName().equals("int")) {
                    IntValue i = (IntValue) val;
                    val = new DecimalValue(i.value(), ctx.line, ctx.column);
                } else {
                    if (!ctx.type.equals(val.getTypeName())) {
                        throw new SemanticException(
                            "No se puede asignar " + val.getTypeName() + " a " + ctx.type,
                            ctx.line,
                            ctx.column
                        );
                    }
                }

            } catch (SemanticException e) {
                currentScope().put(ctx.name, new ErrorValue(ctx.line, ctx.column));
                throw e;

            } catch (Exception e) {
                currentScope().put(ctx.name, new ErrorValue(ctx.line, ctx.column));
                throw new SemanticException(
                    e.getMessage(),
                    ctx.line,
                    ctx.column
                );
            }

        } else {
            val = defaultValue(ctx.type);
        }

        currentScope().put(ctx.name, val);
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(SliceLiteral.Context ctx) {
        java.util.List<ValueWrapper> vals = new java.util.ArrayList<>();

        for (ASTNode node : ctx.values) {
            vals.add(Visit(node));
        }

        return new SliceValue(ctx.type, vals, -1, -1);
    }
    @Override
    public ValueWrapper visit(ForRangeNode.Context ctx) {
        ValueWrapper val = getVariable(ctx.sliceName);

        if (!(val instanceof SliceValue slice)) {
            throw new RuntimeException("range solo puede usarse sobre slices");
        }

        for (int i = 0; i < slice.values().size(); i++) {
            scopes.push(new java.util.HashMap<>());

            currentScope().put(ctx.indexName, new IntValue(i, ctx.line, ctx.column));
            currentScope().put(ctx.valueName, slice.values().get(i));

            try {
                Visit(ctx.body);
            } catch (ContinueException e) {
                scopes.pop();
                continue;
            } catch (BreakException e) {
                scopes.pop();
                break;
            }

            scopes.pop();
        }

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(RuneLiteral.Context ctx) {
        String txt = ctx.value; // ejemplo: 'A'
        char c = txt.charAt(1);
        return new RuneValue(c, ctx.line, ctx.column);
    }
    @Override
    public ValueWrapper visit(TypeOfNode.Context ctx) {

        ValueWrapper value = Visit(ctx.expression);

        return new StringValue(
            value.getTypeName(),
            value.line(),
            value.column()
        );
    }

    @Override
    public ValueWrapper visit(AtoiNode.Context ctx) {

        ValueWrapper value = Visit(ctx.expression);

        if (!(value instanceof StringValue s)) {
            throw new SemanticException(
                "strconv.Atoi solo acepta valores string",
                -1,
                -1
            );
        }

        String text = s.value();

        if (text.contains(".")) {
            throw new SemanticException(
                "Cadena \"" + text + "\" no es un entero valido para Atoi",
                s.line(),
                s.column()
            );
        }

        try {
            int numero = Integer.parseInt(text);

            return new IntValue(
                numero,
                s.line(),
                s.column()
            );

        } catch (NumberFormatException ex) {
            throw new SemanticException(
                "Cadena \"" + text + "\" no puede convertirse a int",
                s.line(),
                s.column()
            );
        }
    }

    @Override
    public ValueWrapper visit(ParseFloatNode.Context ctx) {

        ValueWrapper value = Visit(ctx.expression);

        if (!(value instanceof StringValue s)) {
            throw new RuntimeException(
                "strconv.ParseFloat solo acepta valores string"
            );
        }

        try {

            double numero = Double.parseDouble(s.value());

            return new DecimalValue(
                numero,
                s.line(),
                s.column()
            );

        } catch (NumberFormatException ex) {

            throw new RuntimeException(
                "No se puede convertir \"" +
                s.value() +
                "\" a float64"
            );

        }
    }

    @Override
    public ValueWrapper visit(NilLiteral.Context ctx) {
        return new NilValue(-1, -1);
    }
    @Override
    public ValueWrapper visit(BlockNode.Context ctx) {
        if (insideMethod) {
            Visit(ctx.body);
            return defaultVoid;
        }

        scopes.push(new HashMap<>());

        try {
            Visit(ctx.body);
        } finally {
            scopes.pop();
        }

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(CaseNode.Context ctx) {
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(SwitchNode.Context ctx) {
        ValueWrapper switchValue = Visit(ctx.expression);

        for (ASTNode node : ctx.cases) {
            if (node instanceof CaseNode c) {
                ValueWrapper caseValue = Visit(c.getValue());

                boolean match = switchValue.getTypeName().equals(caseValue.getTypeName())
                        && switchValue.toString().equals(caseValue.toString());

                if (match) {
                    Visit(c.getBody());
                    return defaultVoid;
                }
            }
        }

        if (ctx.defaultBody != null) {
            Visit(ctx.defaultBody);
        }

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(ProgramNode.Context ctx) {

        for (ASTNode node : ctx.functions) {
            if (node instanceof StructDecl s) {
                Visit(s);
            }

            if (node instanceof FunctionDecl f) {
                FunctionDecl.Context fctx = new FunctionDecl.Context(f);
                functions.put(fctx.name, fctx);
            }

            if (node instanceof StructMethodDecl m) {
                StructMethodDecl.Context mctx = new StructMethodDecl.Context(m);
                structMethods.put(methodKey(mctx.structName, mctx.methodName), mctx);
            }
        }

        FunctionDecl.Context main = functions.get("main");

        if (main != null) {
            Visit(main.body);
            return defaultVoid;
        }

        Statments globalStatements = new Statments(null);

        for (ASTNode node : ctx.functions) {
            if (!(node instanceof StructDecl) &&
                !(node instanceof FunctionDecl) &&
                !(node instanceof StructMethodDecl)) {
                globalStatements.add(node);
            }
        }

        Visit(globalStatements);

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(FunctionDecl.Context ctx) {
        functions.put(ctx.name, ctx);
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(ParameterNode.Context ctx) {
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(ReturnNode.Context ctx) {
        ValueWrapper value = Visit(ctx.value);
        throw new ReturnException(value);
    }
    @Override
    public ValueWrapper visit(FunctionCall.Context ctx) {
        FunctionDecl.Context fn = functions.get(ctx.name);

        if (fn == null) {
            throw new RuntimeException("Funcion no definida: " + ctx.name);
        }

        if (ctx.args.size() != fn.params.size()) {
            throw new RuntimeException("Cantidad incorrecta de argumentos en funcion: " + ctx.name);
        }

        java.util.Set<String> paramNames = new java.util.HashSet<>();

        scopes.push(new HashMap<>());

        try {
            for (int i = 0; i < fn.params.size(); i++) {
                ParameterNode param = (ParameterNode) fn.params.get(i);

                if (paramNames.contains(param.name)) {
                    throw new RuntimeException("Parametro duplicado: " + param.name);
                }

                paramNames.add(param.name);

                ValueWrapper argValue = Visit(ctx.args.get(i));
                currentScope().put(param.name, copyIfPrimitive(argValue));
            }

            Visit(fn.body);

        } catch (ReturnException r) {
            scopes.pop();

            if (!fn.returnType.equals("void") && !r.value.getTypeName().equals(fn.returnType)) {
                throw new RuntimeException(
                    "La funcion " + ctx.name + " debe retornar " + fn.returnType +
                    " pero retorno " + r.value.getTypeName()
                );
            }

            return r.value;
        }

        if (!fn.returnType.equals("void")) {
            scopes.pop();
            throw new RuntimeException("La funcion " + ctx.name + " debe retornar " + fn.returnType);
        }

        scopes.pop();
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(SliceAccess.Context ctx) {
        ValueWrapper sliceVal = getVariable(ctx.name);

        if (!(sliceVal instanceof SliceValue slice)) {
            throw new RuntimeException(ctx.name + " no es un slice");
        }

        ValueWrapper indexVal = Visit(ctx.index);

        if (!(indexVal instanceof IntValue idx)) {
            throw new RuntimeException("El indice debe ser int");
        }

        int i = idx.value();

        if (i < 0 || i >= slice.values().size()) {
            throw new RuntimeException("Indice fuera de rango: " + i);
        }

        return slice.values().get(i);
    }
    @Override
    public ValueWrapper visit(SliceAssign.Context ctx) {
        ValueWrapper sliceVal = getVariable(ctx.name);

        if (!(sliceVal instanceof SliceValue slice)) {
            throw new RuntimeException(ctx.name + " no es un slice");
        }

        ValueWrapper indexVal = Visit(ctx.index);

        if (!(indexVal instanceof IntValue idx)) {
            throw new RuntimeException("El indice debe ser int");
        }

        int i = idx.value();

        if (i < 0 || i >= slice.values().size()) {
            throw new RuntimeException("Indice fuera de rango: " + i);
        }

        ValueWrapper newValue = Visit(ctx.value);

        slice.values().set(i, newValue);

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(LenNode.Context ctx) {
        ValueWrapper val = Visit(ctx.expression);

        if (!(val instanceof SliceValue slice)) {
            throw new RuntimeException("len solo acepta slices");
        }

        return new IntValue(slice.values().size(), val.line(), val.column());
    }
    @Override
    public ValueWrapper visit(AppendNode.Context ctx) {
        ValueWrapper sliceVal = Visit(ctx.slice);
        ValueWrapper valueVal = Visit(ctx.value);

        if (!(sliceVal instanceof SliceValue slice)) {
            throw new RuntimeException("append solo acepta slices");
        }

        String tipoEsperado = slice.elementType();

        // Caso 1: slice normal: []int, []string, []float64, etc.
        // Ejemplo: numeros = append(numeros, 4)
        if (!tipoEsperado.startsWith("[]")) {
            if (!valueVal.getTypeName().equals(tipoEsperado)) {
                throw new RuntimeException(
                    "No se puede agregar " + valueVal.getTypeName() +
                    " a " + slice.getTypeName()
                );
            }
        }

        // Caso 2: slice multidimensional: [][]int
        // Ejemplo: mtx1 = append(mtx1, numeros)
        else {
            if (!(valueVal instanceof SliceValue agregado)) {
                throw new RuntimeException(
                    "append esperaba un " + tipoEsperado
                );
            }

            if (!agregado.getTypeName().equals(tipoEsperado)) {
                throw new RuntimeException(
                    "No se puede agregar " + agregado.getTypeName() +
                    " a " + slice.getTypeName()
                );
            }
        }

        java.util.List<ValueWrapper> nuevaLista =
            new java.util.ArrayList<>(slice.values());

        nuevaLista.add(valueVal);

        return new SliceValue(
            slice.elementType(),
            nuevaLista,
            slice.line(),
            slice.column()
        );
    }
    @Override
    public ValueWrapper visit(SlicesIndexNode.Context ctx) {
        ValueWrapper sliceVal = Visit(ctx.slice);
        ValueWrapper searchVal = Visit(ctx.value);

        if (!(sliceVal instanceof SliceValue slice)) {
            throw new RuntimeException("slices.Index requiere un slice");
        }

        for (int i = 0; i < slice.values().size(); i++) {
            if (equalsValues(slice.values().get(i), searchVal)) {
                return new IntValue(i, slice.line(), slice.column());
            }
        }

        return new IntValue(-1, slice.line(), slice.column());
    }
    @Override
    public ValueWrapper visit(StringsJoinNode.Context ctx) {
        ValueWrapper sliceVal = Visit(ctx.slice);
        ValueWrapper sepVal = Visit(ctx.separator);

        if (!(sliceVal instanceof SliceValue slice)) {
            throw new RuntimeException("strings.Join requiere un slice");
        }

        if (!slice.elementType().equals("string")) {
            throw new RuntimeException("strings.Join solo acepta []string");
        }

        if (!(sepVal instanceof StringValue sep)) {
            throw new RuntimeException("El separador de strings.Join debe ser string");
        }

        java.util.List<String> partes = new java.util.ArrayList<>();

        for (ValueWrapper v : slice.values()) {
            if (!(v instanceof StringValue s)) {
                throw new RuntimeException("strings.Join solo acepta strings");
            }

            partes.add(s.value());
        }

        return new StringValue(String.join(sep.value(), partes), slice.line(), slice.column());
    }
    @Override
    public ValueWrapper visit(MultiSliceLiteral.Context ctx) {
        java.util.List<ValueWrapper> rows = new java.util.ArrayList<>();

        for (ASTNode rowNode : ctx.rows) {
            ValueWrapper row = Visit(rowNode);
            rows.add(row);
        }

        return new SliceValue("[]" + ctx.type, rows, -1, -1);
    }
    @Override
    public ValueWrapper visit(MultiSliceAccess.Context ctx) {
        ValueWrapper matrixVal = getVariable(ctx.name);

        if (!(matrixVal instanceof SliceValue matrix)) {
            throw new RuntimeException(ctx.name + " no es un slice multidimensional");
        }

        ValueWrapper rowVal = Visit(ctx.row);
        ValueWrapper colVal = Visit(ctx.column);

        if (!(rowVal instanceof IntValue r)) {
            throw new RuntimeException("El indice de fila debe ser int");
        }

        if (!(colVal instanceof IntValue c)) {
            throw new RuntimeException("El indice de columna debe ser int");
        }

        int rowIndex = r.value();
        int colIndex = c.value();

        if (rowIndex < 0 || rowIndex >= matrix.values().size()) {
            throw new RuntimeException("Indice de fila fuera de rango: " + rowIndex);
        }

        ValueWrapper selectedRow = matrix.values().get(rowIndex);

        if (!(selectedRow instanceof SliceValue row)) {
            throw new RuntimeException("La fila no es un slice");
        }

        if (colIndex < 0 || colIndex >= row.values().size()) {
            throw new RuntimeException("Indice de columna fuera de rango: " + colIndex);
        }

        return row.values().get(colIndex);
    }
    @Override
    public ValueWrapper visit(MultiSliceAssign.Context ctx) {
        ValueWrapper matrixVal = getVariable(ctx.name);

        if (!(matrixVal instanceof SliceValue matrix)) {
            throw new RuntimeException(ctx.name + " no es un slice multidimensional");
        }

        ValueWrapper rowVal = Visit(ctx.row);
        ValueWrapper colVal = Visit(ctx.column);

        if (!(rowVal instanceof IntValue r)) {
            throw new RuntimeException("El indice de fila debe ser int");
        }

        if (!(colVal instanceof IntValue c)) {
            throw new RuntimeException("El indice de columna debe ser int");
        }

        int rowIndex = r.value();
        int colIndex = c.value();

        if (rowIndex < 0 || rowIndex >= matrix.values().size()) {
            throw new RuntimeException("Indice de fila fuera de rango: " + rowIndex);
        }

        ValueWrapper selectedRow = matrix.values().get(rowIndex);

        if (!(selectedRow instanceof SliceValue row)) {
            throw new RuntimeException("La fila no es un slice");
        }

        if (colIndex < 0 || colIndex >= row.values().size()) {
            throw new RuntimeException("Indice de columna fuera de rango: " + colIndex);
        }

        ValueWrapper newValue = Visit(ctx.value);
        row.values().set(colIndex, newValue);

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(StructDecl.Context ctx) {
        if (ctx.fields == null || ctx.fields.isEmpty()) {
            throw new RuntimeException("El struct " + ctx.name + " debe tener al menos un atributo");
        }

        structDefs.put(ctx.name, ctx);
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(StructVarDecl.Context ctx) {
        StructDecl.Context def = structDefs.get(ctx.structType);

        if (def == null) {
            throw new RuntimeException("Struct no definido: " + ctx.structType);
        }

        Map<String, ValueWrapper> attrs = new HashMap<>();

        for (StructField field : def.fields) {
            attrs.put(field.name, defaultValue(field.type));
        }

        for (StructAssignment assign : ctx.values) {
            if (!attrs.containsKey(assign.name)) {
                throw new RuntimeException("El struct " + ctx.structType + " no tiene atributo: " + assign.name);
            }

            String fieldType = null;

            for (StructField field : def.fields) {
                if (field.name.equals(assign.name)) {
                    fieldType = field.type;
                    break;
                }
            }

            String previous = expectedStructType;
            expectedStructType = fieldType;

            ValueWrapper val = Visit(assign.value);

            expectedStructType = previous;

            attrs.put(assign.name, val);
        }

        currentScope().put(ctx.varName, new StructValue(ctx.structType, attrs, -1, -1));
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(StructAccess.Context ctx) {
        ValueWrapper val = getVariable(ctx.varName);

        if (!(val instanceof StructValue s)) {
            throw new RuntimeException(ctx.varName + " no es un struct");
        }

        if (!s.attributes().containsKey(ctx.fieldName)) {
            throw new RuntimeException("El struct " + s.structName() + " no tiene atributo: " + ctx.fieldName);
        }

        return s.attributes().get(ctx.fieldName);
    }
    @Override
    public ValueWrapper visit(StructAssign.Context ctx) {
        ValueWrapper val = getVariable(ctx.varName);

        if (val == null) {
            throw new RuntimeException("Variable no definida: " + ctx.varName);
        }

        if (!(val instanceof StructValue s)) {
            throw new RuntimeException(ctx.varName + " no es un struct, es: " + val.getTypeName());
        }

        if (!s.attributes().containsKey(ctx.fieldName)) {
            throw new RuntimeException("El struct " + s.structName() + " no tiene atributo: " + ctx.fieldName);
        }

        ValueWrapper newVal = Visit(ctx.value);
        s.attributes().put(ctx.fieldName, newVal);

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(StructInlineLiteral.Context ctx) {
        if (expectedStructType == null) {
            throw new RuntimeException("No se puede inferir el tipo del struct anidado");
        }

        StructDecl.Context def = structDefs.get(expectedStructType);

        if (def == null) {
            throw new RuntimeException("Struct no definido: " + expectedStructType);
        }

        Map<String, ValueWrapper> attrs = new HashMap<>();

        for (StructField field : def.fields) {
            attrs.put(field.name, defaultValue(field.type));
        }

        for (StructAssignment assign : ctx.values) {
            if (!attrs.containsKey(assign.name)) {
                throw new RuntimeException("El struct " + expectedStructType + " no tiene atributo: " + assign.name);
            }

            ValueWrapper val = Visit(assign.value);
            attrs.put(assign.name, val);
        }

        return new StructValue(expectedStructType, attrs, -1, -1);
    }
    @Override
    public ValueWrapper visit(StructAccessExpr.Context ctx) {
        ValueWrapper obj = Visit(ctx.object);

        if (!(obj instanceof StructValue s)) {
            throw new RuntimeException("No es un struct");
        }

        if (!s.attributes().containsKey(ctx.fieldName)) {
            throw new RuntimeException("El struct " + s.structName() + " no tiene atributo: " + ctx.fieldName);
        }

        return s.attributes().get(ctx.fieldName);
    }
    @Override
    public ValueWrapper visit(StructMethodDecl.Context ctx) {
        structMethods.put(methodKey(ctx.structName, ctx.methodName), ctx);
        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(StructMethodCall.Context ctx) {
        ValueWrapper obj = Visit(ctx.object);

        if (!(obj instanceof StructValue structObj)) {
            throw new RuntimeException("La llamada de metodo requiere un struct");
        }

        StructMethodDecl.Context method =
                structMethods.get(methodKey(structObj.structName(), ctx.methodName));

        if (method == null) {
            throw new RuntimeException(
                "Metodo no definido: " + structObj.structName() + "." + ctx.methodName
            );
        }

        if (ctx.args.size() != method.params.size()) {
            throw new RuntimeException("Cantidad incorrecta de argumentos en metodo: " + ctx.methodName);
        }

        Map<String, ValueWrapper> methodScope = new HashMap<>();
        methodScope.put(method.referenceName, structObj);

        scopes.push(methodScope);

        boolean previousInsideMethod = insideMethod;
        insideMethod = true;

        try {
            for (int i = 0; i < method.params.size(); i++) {
                ParameterNode param = (ParameterNode) method.params.get(i);
                ValueWrapper argValue = Visit(ctx.args.get(i));
                currentScope().put(param.name, argValue);
            }

            Visit(method.body);

        } catch (ReturnException r) {
            return r.value;
        } finally {
            insideMethod = previousInsideMethod;
            scopes.pop();
        }

        return defaultVoid;
    }
    @Override
    public ValueWrapper visit(StructTypedLiteral.Context ctx) {
        StructDecl.Context def = structDefs.get(ctx.structType);

        if (def == null) {
            throw new RuntimeException("Struct no definido: " + ctx.structType);
        }

        Map<String, ValueWrapper> attrs = new HashMap<>();

        for (StructField field : def.fields) {
            attrs.put(field.name, defaultValue(field.type));
        }

        for (StructAssignment assign : ctx.values) {
            if (!attrs.containsKey(assign.name)) {
                throw new RuntimeException("El struct " + ctx.structType + " no tiene atributo: " + assign.name);
            }

            String fieldType = null;

            for (StructField field : def.fields) {
                if (field.name.equals(assign.name)) {
                    fieldType = field.type;
                    break;
                }
            }

            String previous = expectedStructType;
            expectedStructType = fieldType;

            ValueWrapper val = Visit(assign.value);

            expectedStructType = previous;

            attrs.put(assign.name, val);
        }

        return new StructValue(ctx.structType, attrs, -1, -1);
    }
}
