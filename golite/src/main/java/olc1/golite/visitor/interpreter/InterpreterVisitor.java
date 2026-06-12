package olc1.golite.visitor.interpreter;

import java.util.HashMap;
import java.util.Map;

import olc1.golite.ast.ASTNode;
import olc1.golite.ast.exp.*;
import olc1.golite.ast.stm.*;
import olc1.golite.visitor.Visitor;
import olc1.golite.visitor.interpreter.value.*;

public class InterpreterVisitor implements Visitor<ValueWrapper> {
    public String output = "";
    private final ValueWrapper defaultVoid = new VoidValue(-1, -1);
    private final Map<String, ValueWrapper> variables = new HashMap<>();

    public ValueWrapper Visit(ASTNode node) {
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
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() + r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue(l.value() + r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() + r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() + r.value(), l.line(), l.column());
            default -> throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " + " + right.getTypeName());
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
            Visit(statment);
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
        return new StringValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(VarRef.Context ctx) {
        ValueWrapper val = variables.get(ctx.name);
        if (val == null) throw new RuntimeException("Variable no definida: " + ctx.name);
        return val;
    }

    @Override
    public ValueWrapper visit(Assign.Context ctx) {
        ValueWrapper val = Visit(ctx.value);
        variables.put(ctx.name, val);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(IfNode.Context ctx) {
        ValueWrapper cond = Visit(ctx.condition);
    if (!(cond instanceof BoolValue)) {
        throw new RuntimeException("La condicion del if debe ser booleana");
    }

    if (((BoolValue) cond).value()) {
        Visit(ctx.body);
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
}
