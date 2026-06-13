package olc1.golite.visitor.interpreter;

import java.util.HashMap;
import java.util.Map;

import olc1.golite.ast.ASTNode;
import olc1.golite.ast.exp.*;
import olc1.golite.ast.stm.*;
import olc1.golite.reports.GoliteError;
import olc1.golite.visitor.Visitor;
import olc1.golite.visitor.interpreter.value.*;

public class InterpreterVisitor implements Visitor<ValueWrapper> {
    public String output = "";
    private final ValueWrapper defaultVoid = new VoidValue(-1, -1);
    private final Map<String, ValueWrapper> variables = new HashMap<>();    
    public final java.util.List<GoliteError> semanticErrors = new java.util.ArrayList<>();
    private final java.util.Deque<Map<String, ValueWrapper>> scopes = new java.util.ArrayDeque<>();

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

        // VALIDACION DE DIVISION POR 0
        if ((right instanceof IntValue r1 && r1.value() == 0) ||
            (right instanceof DecimalValue r2 && r2.value() == 0.0)) {
            throw new RuntimeException("Error: Division por 0");
        }

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue(l.value() / r.value(), l.line(), l.column());

            case IntValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() / r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() / r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() / r.value(), l.line(), l.column());

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " / " + right.getTypeName()
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
        scopes.push(new HashMap<>());

        try {
            for (ASTNode statment : ctx.statements) {
                Visit(statment);
            }
        } finally {
            scopes.pop();
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
            throw new RuntimeException("Variable no definida: " + ctx.name);
        }

        return val;
    }

    @Override
    public ValueWrapper visit(Assign.Context ctx) {
        ValueWrapper val = Visit(ctx.value);
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

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());

            case BoolValue l when right instanceof BoolValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());

            case StringValue l when right instanceof StringValue r ->
                new BoolValue(l.value().equals(r.value()), l.line(), l.column());

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " == " + right.getTypeName()
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

            case BoolValue l when right instanceof BoolValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());

            case StringValue l when right instanceof StringValue r ->
                new BoolValue(!l.value().equals(r.value()), l.line(), l.column());

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

            default -> throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " <= " + right.getTypeName()
            );
        };
    }
    @Override
    public ValueWrapper visit(And.Context ctx) {
        ValueWrapper left = Visit(ctx.left);

        if (!(left instanceof BoolValue l)) {
            throw new RuntimeException("Operacion invalida: && requiere booleanos");
        }

        if (!l.value()) {
            return new BoolValue(false, l.line(), l.column());
        }

        ValueWrapper right = Visit(ctx.right);

        if (!(right instanceof BoolValue r)) {
            throw new RuntimeException("Operacion invalida: && requiere booleanos");
        }

        return new BoolValue(r.value(), r.line(), r.column());
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
        throw new RuntimeException("Variable no definida: " + ctx.name);
    }

    ValueWrapper right = Visit(ctx.value);

    ValueWrapper result = switch (current) {
        case IntValue l when right instanceof IntValue r ->
            new IntValue(l.value() + r.value(), l.line(), l.column());

        case IntValue l when right instanceof DecimalValue r ->
            new DecimalValue(l.value() + r.value(), l.line(), l.column());

        case DecimalValue l when right instanceof IntValue r ->
            new DecimalValue(l.value() + r.value(), l.line(), l.column());

        case DecimalValue l when right instanceof DecimalValue r ->
            new DecimalValue(l.value() + r.value(), l.line(), l.column());

        case StringValue l when right instanceof StringValue r ->
            new StringValue(l.value() + r.value(), l.line(), l.column());

        default -> throw new RuntimeException(
            "Operacion invalida: " + current.getTypeName() + " += " + right.getTypeName()
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

        int guard = 0;

        while (true) {
            guard++;
            if (guard > 100000) {
                throw new RuntimeException("Posible ciclo infinito en for");
            }

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
        for (int i = 0; i < ctx.arguments.size(); i++) {
            if (i > 0) output += " ";
            output += Visit(ctx.arguments.get(i)).toString();
        }
        output += "\n";
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(BreakNode.Context ctx) {
        throw new BreakException();
    }

    @Override
    public ValueWrapper visit(ContinueNode.Context ctx) {
        throw new ContinueException();
    }
    @Override
    public ValueWrapper visit(VarDeclInfer.Context ctx) {
        ValueWrapper val = Visit(ctx.value);
        currentScope().put(ctx.name, val);
        return defaultVoid;
    }
@Override
public ValueWrapper visit(VarDecl.Context ctx) {
    ValueWrapper val;

    if (ctx.value != null) {
        val = Visit(ctx.value);
    } else {
        val = switch (ctx.type) {
            case "int" -> new IntValue(0, ctx.line, ctx.column);
            case "float64" -> new DecimalValue(0.0, ctx.line, ctx.column);
            case "string" -> new StringValue("", ctx.line, ctx.column);
            case "bool" -> new BoolValue(false, ctx.line, ctx.column);
            default -> new VoidValue(ctx.line, ctx.column);
        };
    }

    currentScope().put(ctx.name, val);
    return defaultVoid;
}
}
