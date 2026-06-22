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
    for (ASTNode statment : ctx.statements) {
        if (statment != null) {
            Visit(statment);
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

            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());

            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());

            case BoolValue l when right instanceof BoolValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());

            case StringValue l when right instanceof StringValue r ->
                new BoolValue(l.value().equals(r.value()), l.line(), l.column());

            case RuneValue l when right instanceof RuneValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());

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
                case "[]int" -> new SliceValue("int", new java.util.ArrayList<>(), ctx.line, ctx.column);
                case "[]float64" -> new SliceValue("float64", new java.util.ArrayList<>(), ctx.line, ctx.column);
                case "[]string" -> new SliceValue("string", new java.util.ArrayList<>(), ctx.line, ctx.column);
                case "[]bool" -> new SliceValue("bool", new java.util.ArrayList<>(), ctx.line, ctx.column);
                case "[]rune" -> new SliceValue("rune", new java.util.ArrayList<>(), ctx.line, ctx.column);
                default -> new VoidValue(ctx.line, ctx.column);
            };
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
        ValueWrapper val = Visit(ctx.expression);
        return new StringValue(val.getTypeName(), val.line(), val.column());
    }

    @Override
    public ValueWrapper visit(AtoiNode.Context ctx) {
        ValueWrapper val = Visit(ctx.expression);

        if (!(val instanceof StringValue s)) {
            throw new RuntimeException("strconv.Atoi requiere string");
        }

        try {
            return new IntValue(Integer.parseInt(s.value()), s.line(), s.column());
        } catch (NumberFormatException e) {
            throw new RuntimeException("No se puede convertir a int: " + s.value());
        }
    }

    @Override
    public ValueWrapper visit(ParseFloatNode.Context ctx) {
        ValueWrapper val = Visit(ctx.expression);

        if (!(val instanceof StringValue s)) {
            throw new RuntimeException("strconv.ParseFloat requiere string");
        }

        try {
            return new DecimalValue(Double.parseDouble(s.value()), s.line(), s.column());
        } catch (NumberFormatException e) {
            throw new RuntimeException("No se puede convertir a float64: " + s.value());
        }
    }

    @Override
    public ValueWrapper visit(NilLiteral.Context ctx) {
        return new NilValue(-1, -1);
    }
    @Override
    public ValueWrapper visit(BlockNode.Context ctx) {
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
        for (ASTNode fn : ctx.functions) {
            if (fn instanceof FunctionDecl f) {
                FunctionDecl.Context fctx = new FunctionDecl.Context(f);
                functions.put(fctx.name, fctx);
            }
        }

        FunctionDecl.Context main = functions.get("main");

        if (main == null) {
            throw new RuntimeException("No existe funcion main");
        }

        Visit(main.body);

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

        scopes.push(new HashMap<>());

        try {
            for (int i = 0; i < fn.params.size(); i++) {
                ParameterNode param = (ParameterNode) fn.params.get(i);
                ValueWrapper argValue = Visit(ctx.args.get(i));

                currentScope().put(param.name, argValue);
            }

            Visit(fn.body);

        } catch (ReturnException r) {
            scopes.pop();
            return r.value;
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
        ValueWrapper value = Visit(ctx.value);

        if (!(sliceVal instanceof SliceValue slice)) {
            throw new RuntimeException("append solo acepta slices");
        }

        java.util.List<ValueWrapper> nuevaLista = new java.util.ArrayList<>(slice.values());
        nuevaLista.add(value);

        return new SliceValue(slice.elementType(), nuevaLista, slice.line(), slice.column());
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
}
