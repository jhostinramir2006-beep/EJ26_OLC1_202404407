package olc1.golite.visitor.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import olc1.golite.ast.ASTNode;
import olc1.golite.ast.exp.*;
import olc1.golite.ast.stm.*;
import olc1.golite.visitor.Visitor;
import olc1.golite.visitor.interpreter.value.*;

public class InterpreterVisitor implements Visitor<ValueWrapper> {

    // --------------------------------------------------------
    // Salida del programa (lo que se muestra en la consola)
    // --------------------------------------------------------
    public String output = "";

    // --------------------------------------------------------
    // Valor void por defecto para sentencias sin retorno
    // --------------------------------------------------------
    private final ValueWrapper defaultVoid = new VoidValue(-1, -1);

    // --------------------------------------------------------
    // MODIFICADO: Sistema de scopes (entornos anidados)
    // Antes: un solo Map<String, ValueWrapper> variables
    // Ahora: una pila de entornos para manejar ambitos locales/globales
    // Ejemplo: variables dentro de if, for, bloques independientes
    // --------------------------------------------------------
    private final List<Map<String, ValueWrapper>> scopes = new ArrayList<>();

    // --------------------------------------------------------
    // AGREGADO: Flags de control de flujo
    // Se usan para implementar break, continue y return
    // --------------------------------------------------------
    private boolean breakFlag    = false;
    private boolean continueFlag = false;
    private boolean returnFlag   = false;
    private ValueWrapper returnValue = null;

    // --------------------------------------------------------
    // AGREGADO: Flag para saber si estamos dentro de un for
    // Necesario para validar break y continue fuera de bucle
    // --------------------------------------------------------
    private int loopDepth = 0;

    // --------------------------------------------------------
    // Constructor: inicializa con el scope global
    // --------------------------------------------------------
    public InterpreterVisitor() {
        pushScope(); // scope global
    }

    // --------------------------------------------------------
    // AGREGADO: Manejo de scopes
    // --------------------------------------------------------

    // Crear un nuevo scope (al entrar a un bloque)
    private void pushScope() {
        scopes.add(new HashMap<>());
    }

    // Eliminar el scope actual (al salir de un bloque)
    private void popScope() {
        if (scopes.size() > 1) {
            scopes.remove(scopes.size() - 1);
        }
    }

    // Buscar una variable en todos los scopes (del mas interno al mas externo)
    private ValueWrapper getVariable(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        return null;
    }

    // Asignar valor a una variable existente (busca en todos los scopes)
    // Si no existe en ningun scope, la crea en el scope actual
    private void setVariable(String name, ValueWrapper value) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                scopes.get(i).put(name, value);
                return;
            }
        }
        // Si no existe en ningun scope, la declara en el scope actual
        scopes.get(scopes.size() - 1).put(name, value);
    }

    // Declarar una variable NUEVA en el scope actual
    private void declareVariable(String name, ValueWrapper value) {
        scopes.get(scopes.size() - 1).put(name, value);
    }

    // --------------------------------------------------------
    // Metodo principal para visitar un nodo
    // --------------------------------------------------------
    public ValueWrapper Visit(ASTNode node) {
        if (node == null) return defaultVoid;
        return node.accept(this);
    }

    // ========================================================
    // EXPRESIONES LITERALES
    // ========================================================

    @Override
    public ValueWrapper visit(Integers.Context ctx) {
        return new IntValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(Decimal.Context ctx) {
        return new DecimalValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(BoolLiteral.Context ctx) {
        return new BoolValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(StringLiteral.Context ctx) {
        // Quitar las comillas dobles del string
        String raw = ctx.value;
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
            raw = raw.substring(1, raw.length() - 1);
        }
        // Procesar secuencias de escape
        raw = raw.replace("\\n", "\n")
                 .replace("\\t", "\t")
                 .replace("\\r", "\r")
                 .replace("\\\\", "\\")
                 .replace("\\\"", "\"");
        return new StringValue(raw, ctx.line, ctx.column);
    }

    // AGREGADO: Literal rune
    @Override
    public ValueWrapper visit(RuneLiteral.Context ctx) {
        return new RuneValue(ctx.charValue, ctx.line, ctx.column);
    }

    // AGREGADO: Literal nil
    @Override
    public ValueWrapper visit(NilLiteral.Context ctx) {
        return new NilValue(ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(Paren.Context ctx) {
        return Visit(ctx.expression);
    }

    // ========================================================
    // OPERADORES ARITMETICOS
    // ========================================================

    @Override
    public ValueWrapper visit(Add.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        // AGREGADO: Soporte para concatenacion de strings
        if (left instanceof StringValue l && right instanceof StringValue r) {
            return new StringValue(l.value() + r.value(), l.line(), l.column());
        }

        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() + r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue((double)l.value() + r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() + (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() + r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: Operacion invalida: " + left.getTypeName() + " + " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(Sub.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() - r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue((double)l.value() - r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() - (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() - r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: Operacion invalida: " + left.getTypeName() + " - " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(Mul.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() * r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue((double)l.value() * r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() * (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() * r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: Operacion invalida: " + left.getTypeName() + " * " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(Div.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        // AGREGADO: Verificacion de division por cero
        if (right instanceof IntValue r && r.value() == 0) {
            throw new RuntimeException("Error Semantico: Division por cero en linea " + r.line());
        }
        if (right instanceof DecimalValue r && r.value() == 0.0) {
            throw new RuntimeException("Error Semantico: Division por cero en linea " + r.line());
        }

        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() / r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue((double)l.value() / r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() / (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() / r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: Operacion invalida: " + left.getTypeName() + " / " + right.getTypeName()
            );
        };
    }

    // AGREGADO: Modulo %
    @Override
    public ValueWrapper visit(Mod.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        // Verificacion de modulo por cero
        if (right instanceof IntValue r && r.value() == 0) {
            throw new RuntimeException("Error Semantico: Modulo por cero en linea " + r.line());
        }

        return switch (left) {
            case IntValue l when right instanceof IntValue r -> new IntValue(l.value() % r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: Operacion invalida: " + left.getTypeName() + " % " + right.getTypeName() + ". Solo se permite int % int"
            );
        };
    }

    @Override
    public ValueWrapper visit(Negate.Context ctx) {
        ValueWrapper operand = Visit(ctx.expression);
        return switch (operand) {
            case IntValue     v -> new IntValue(-v.value(), v.line(), v.column());
            case DecimalValue v -> new DecimalValue(-v.value(), v.line(), v.column());
            default -> throw new RuntimeException(
                "Error Semantico: Operacion invalida: -" + operand.getTypeName()
            );
        };
    }

    // ========================================================
    // OPERADORES DE COMPARACION
    // AGREGADOS: Todos son nuevos
    // ========================================================

    @Override
    public ValueWrapper visit(Equal.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new BoolValue(l.value() == r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new BoolValue((double)l.value() == r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new BoolValue(l.value() == (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new BoolValue(l.value() == r.value(), l.line(), l.column());
            case BoolValue    l when right instanceof BoolValue    r -> new BoolValue(l.value() == r.value(), l.line(), l.column());
            case StringValue  l when right instanceof StringValue  r -> new BoolValue(l.value().equals(r.value()), l.line(), l.column());
            case RuneValue    l when right instanceof RuneValue    r -> new BoolValue(l.value() == r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: No se puede comparar " + left.getTypeName() + " == " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(NotEqual.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new BoolValue(l.value() != r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new BoolValue((double)l.value() != r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new BoolValue(l.value() != (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new BoolValue(l.value() != r.value(), l.line(), l.column());
            case BoolValue    l when right instanceof BoolValue    r -> new BoolValue(l.value() != r.value(), l.line(), l.column());
            case StringValue  l when right instanceof StringValue  r -> new BoolValue(!l.value().equals(r.value()), l.line(), l.column());
            case RuneValue    l when right instanceof RuneValue    r -> new BoolValue(l.value() != r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: No se puede comparar " + left.getTypeName() + " != " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(LessThan.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new BoolValue(l.value() < r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new BoolValue((double)l.value() < r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new BoolValue(l.value() < (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new BoolValue(l.value() < r.value(), l.line(), l.column());
            case RuneValue    l when right instanceof RuneValue    r -> new BoolValue(l.value() < r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: No se puede comparar " + left.getTypeName() + " < " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(GreaterThan.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new BoolValue(l.value() > r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new BoolValue((double)l.value() > r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new BoolValue(l.value() > (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new BoolValue(l.value() > r.value(), l.line(), l.column());
            case RuneValue    l when right instanceof RuneValue    r -> new BoolValue(l.value() > r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: No se puede comparar " + left.getTypeName() + " > " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(LessEqual.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new BoolValue(l.value() <= r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new BoolValue((double)l.value() <= r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new BoolValue(l.value() <= (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new BoolValue(l.value() <= r.value(), l.line(), l.column());
            case RuneValue    l when right instanceof RuneValue    r -> new BoolValue(l.value() <= r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: No se puede comparar " + left.getTypeName() + " <= " + right.getTypeName()
            );
        };
    }

    @Override
    public ValueWrapper visit(GreaterEqual.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new BoolValue(l.value() >= r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new BoolValue((double)l.value() >= r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new BoolValue(l.value() >= (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new BoolValue(l.value() >= r.value(), l.line(), l.column());
            case RuneValue    l when right instanceof RuneValue    r -> new BoolValue(l.value() >= r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: No se puede comparar " + left.getTypeName() + " >= " + right.getTypeName()
            );
        };
    }

    // ========================================================
    // OPERADORES LOGICOS
    // AGREGADOS: Todos son nuevos
    // ========================================================

    @Override
    public ValueWrapper visit(And.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        if (!(left instanceof BoolValue)) {
            throw new RuntimeException("Error Semantico: El operador && requiere bool, se recibio: " + left.getTypeName());
        }
        // Short-circuit: si left es false, no evalua right
        if (!((BoolValue) left).value()) return new BoolValue(false, -1, -1);

        ValueWrapper right = Visit(ctx.right);
        if (!(right instanceof BoolValue)) {
            throw new RuntimeException("Error Semantico: El operador && requiere bool, se recibio: " + right.getTypeName());
        }
        return new BoolValue(((BoolValue) right).value(), -1, -1);
    }

    @Override
    public ValueWrapper visit(Or.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        if (!(left instanceof BoolValue)) {
            throw new RuntimeException("Error Semantico: El operador || requiere bool, se recibio: " + left.getTypeName());
        }
        // Short-circuit: si left es true, no evalua right
        if (((BoolValue) left).value()) return new BoolValue(true, -1, -1);

        ValueWrapper right = Visit(ctx.right);
        if (!(right instanceof BoolValue)) {
            throw new RuntimeException("Error Semantico: El operador || requiere bool, se recibio: " + right.getTypeName());
        }
        return new BoolValue(((BoolValue) right).value(), -1, -1);
    }

    @Override
    public ValueWrapper visit(Not.Context ctx) {
        ValueWrapper val = Visit(ctx.expression);
        if (!(val instanceof BoolValue b)) {
            throw new RuntimeException("Error Semantico: El operador ! requiere bool, se recibio: " + val.getTypeName());
        }
        return new BoolValue(!b.value(), -1, -1);
    }

    // ========================================================
    // REFERENCIAS A VARIABLES
    // MODIFICADO: Ahora usa el sistema de scopes
    // ========================================================

    @Override
    public ValueWrapper visit(VarRef.Context ctx) {
        ValueWrapper val = getVariable(ctx.name);
        if (val == null) {
            throw new RuntimeException(
                "Error Semantico: Variable no definida: '" + ctx.name + "' en linea " + ctx.line
            );
        }
        return val;
    }

    // ========================================================
    // DECLARACION DE VARIABLES
    // AGREGADOS: VarDecl y VarDeclInfer son nuevos
    // ========================================================

    // AGREGADO: var x int = 5  o  var x int
    @Override
    public ValueWrapper visit(VarDecl.Context ctx) {
        // Verificar que no exista en el scope actual
        if (scopes.get(scopes.size() - 1).containsKey(ctx.name)) {
            throw new RuntimeException(
                "Error Semantico: La variable '" + ctx.name + "' ya fue declarada en este ambito, linea " + ctx.line
            );
        }

        ValueWrapper val;
        if (ctx.value != null) {
            val = Visit(ctx.value);
            // Verificar compatibilidad de tipos
            val = checkTypeCompatibility(ctx.type, val, ctx.line);
        } else {
            // Valor por defecto segun el tipo
            val = getDefaultValue(ctx.type);
        }
        declareVariable(ctx.name, val);
        return defaultVoid;
    }

    // AGREGADO: x := 5  (inferencia de tipo)
    @Override
    public ValueWrapper visit(VarDeclInfer.Context ctx) {
        // Verificar que no exista en el scope actual
        if (scopes.get(scopes.size() - 1).containsKey(ctx.name)) {
            throw new RuntimeException(
                "Error Semantico: La variable '" + ctx.name + "' ya fue declarada en este ambito, linea " + ctx.line
            );
        }
        ValueWrapper val = Visit(ctx.value);
        declareVariable(ctx.name, val);
        return defaultVoid;
    }

    // ========================================================
    // ASIGNACIONES
    // MODIFICADO: Ahora usa scopes y verifica tipos estaticos
    // ========================================================

    @Override
    public ValueWrapper visit(Assign.Context ctx) {
        ValueWrapper current = getVariable(ctx.name);
        if (current == null) {
            throw new RuntimeException(
                "Error Semantico: Variable no declarada: '" + ctx.name + "' en linea " + ctx.line
            );
        }
        ValueWrapper newVal = Visit(ctx.value);

        // Verificar que el tipo sea compatible
        if (!typesCompatible(current, newVal)) {
            throw new RuntimeException(
                "Error Semantico: No se puede asignar " + newVal.getTypeName() +
                " a variable de tipo " + current.getTypeName() +
                " (" + ctx.name + ") en linea " + ctx.line
            );
        }
        // Conversion implicita int → float64
        if (current instanceof DecimalValue && newVal instanceof IntValue iv) {
            newVal = new DecimalValue((double) iv.value(), iv.line(), iv.column());
        }
        setVariable(ctx.name, newVal);
        return defaultVoid;
    }

    // AGREGADO: x += 5
    @Override
    public ValueWrapper visit(PlusAssign.Context ctx) {
        ValueWrapper current = getVariable(ctx.name);
        if (current == null) {
            throw new RuntimeException("Error Semantico: Variable no declarada: '" + ctx.name + "'");
        }
        ValueWrapper right = Visit(ctx.value);
        // Reusar la logica de Add
        ValueWrapper result = Visit(new Add(
            new DummyNode(current),
            new DummyNode(right)
        ).accept(this) != null ? new Add(new DummyNode(current), new DummyNode(right)) : new Add(new DummyNode(current), new DummyNode(right)));

        // Forma directa sin crear nodos dummy:
        result = addValues(current, right, ctx.line);
        setVariable(ctx.name, result);
        return defaultVoid;
    }

    // AGREGADO: x -= 5
    @Override
    public ValueWrapper visit(MinusAssign.Context ctx) {
        ValueWrapper current = getVariable(ctx.name);
        if (current == null) {
            throw new RuntimeException("Error Semantico: Variable no declarada: '" + ctx.name + "'");
        }
        ValueWrapper right = Visit(ctx.value);
        ValueWrapper result = subValues(current, right, ctx.line);
        setVariable(ctx.name, result);
        return defaultVoid;
    }

    // AGREGADO: x++
    @Override
    public ValueWrapper visit(Increment.Context ctx) {
        ValueWrapper current = getVariable(ctx.name);
        if (current == null) {
            throw new RuntimeException("Error Semantico: Variable no declarada: '" + ctx.name + "'");
        }
        ValueWrapper result = switch (current) {
            case IntValue     v -> new IntValue(v.value() + 1, v.line(), v.column());
            case DecimalValue v -> new DecimalValue(v.value() + 1.0, v.line(), v.column());
            default -> throw new RuntimeException(
                "Error Semantico: ++ no aplica a tipo " + current.getTypeName()
            );
        };
        setVariable(ctx.name, result);
        return defaultVoid;
    }

    // AGREGADO: x--
    @Override
    public ValueWrapper visit(Decrement.Context ctx) {
        ValueWrapper current = getVariable(ctx.name);
        if (current == null) {
            throw new RuntimeException("Error Semantico: Variable no declarada: '" + ctx.name + "'");
        }
        ValueWrapper result = switch (current) {
            case IntValue     v -> new IntValue(v.value() - 1, v.line(), v.column());
            case DecimalValue v -> new DecimalValue(v.value() - 1.0, v.line(), v.column());
            default -> throw new RuntimeException(
                "Error Semantico: -- no aplica a tipo " + current.getTypeName()
            );
        };
        setVariable(ctx.name, result);
        return defaultVoid;
    }

    // ========================================================
    // SENTENCIAS DE CONTROL
    // ========================================================

    // MODIFICADO: Ahora maneja else y else if, y usa scopes
    @Override
    public ValueWrapper visit(IfNode.Context ctx) {
        ValueWrapper cond = Visit(ctx.condition);
        if (!(cond instanceof BoolValue b)) {
            throw new RuntimeException(
                "Error Semantico: La condicion del if debe ser bool, se recibio: " + cond.getTypeName()
            );
        }
        if (b.value()) {
            pushScope();
            Visit(ctx.body);
            popScope();
        } else if (ctx.elseBody != null) {
            pushScope();
            Visit(ctx.elseBody);
            popScope();
        }
        return defaultVoid;
    }

    // AGREGADO: for en sus 2 variantes
    @Override
    public ValueWrapper visit(ForNode.Context ctx) {
        pushScope();
        loopDepth++;

        // Ejecutar inicializacion si existe (for clasico)
        if (ctx.init != null) {
            Visit(ctx.init);
        }

        // Bucle principal
        while (true) {
            // Evaluar condicion
            ValueWrapper cond = Visit(ctx.condition);
            if (!(cond instanceof BoolValue b)) {
                throw new RuntimeException(
                    "Error Semantico: La condicion del for debe ser bool, se recibio: " + cond.getTypeName()
                );
            }
            if (!b.value()) break;

            // Ejecutar cuerpo
            pushScope();
            Visit(ctx.body);
            popScope();

            // Verificar break
            if (breakFlag) {
                breakFlag = false;
                break;
            }

            // Verificar return
            if (returnFlag) break;

            // Limpiar continue para la siguiente iteracion
            continueFlag = false;

            // Ejecutar actualizacion si existe (for clasico)
            if (ctx.update != null) {
                Visit(ctx.update);
            }
        }

        loopDepth--;
        popScope();
        return defaultVoid;
    }

    // AGREGADO: break
    @Override
    public ValueWrapper visit(BreakNode.Context ctx) {
        if (loopDepth == 0) {
            throw new RuntimeException(
                "Error Semantico: 'break' usado fuera de un bucle en linea " + ctx.line
            );
        }
        breakFlag = true;
        return defaultVoid;
    }

    // AGREGADO: continue
    @Override
    public ValueWrapper visit(ContinueNode.Context ctx) {
        if (loopDepth == 0) {
            throw new RuntimeException(
                "Error Semantico: 'continue' usado fuera de un bucle en linea " + ctx.line
            );
        }
        continueFlag = true;
        return defaultVoid;
    }

    // AGREGADO: return
    @Override
    public ValueWrapper visit(ReturnNode.Context ctx) {
        if (ctx.value != null) {
            returnValue = Visit(ctx.value);
        } else {
            returnValue = defaultVoid;
        }
        returnFlag = true;
        return returnValue;
    }

    // ========================================================
    // LISTA DE INSTRUCCIONES
    // MODIFICADO: Ahora respeta los flags de break/continue/return
    // ========================================================
    @Override
    public ValueWrapper visit(Statments.Context ctx) {
        for (ASTNode statement : ctx.statements) {
            if (statement == null) continue;
            Visit(statement);
            // Detener ejecucion si se activo break, continue o return
            if (breakFlag || continueFlag || returnFlag) break;
        }
        return defaultVoid;
    }

    // ========================================================
    // FUNCIONES EMBEBIDAS
    // ========================================================

    // MODIFICADO: Antes era imprimir(expr), ahora es fmt.Println(args...)
    // ELIMINADO: visit(Imprimir.Context ctx)
    // AGREGADO: visit(FmtPrintln.Context ctx)
    @Override
    public ValueWrapper visit(FmtPrintln.Context ctx) {
        if (ctx.arguments.isEmpty()) {
            // Sin argumentos: solo imprime salto de linea
            output += "\n";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ctx.arguments.size(); i++) {
                ValueWrapper val = Visit(ctx.arguments.get(i));
                if (i > 0) sb.append(" ");
                sb.append(formatValue(val));
            }
            output += sb.toString() + "\n";
        }
        return defaultVoid;
    }

    // AGREGADO: Llamada a funcion (por ahora solo funciones embebidas especiales)
    @Override
    public ValueWrapper visit(FuncCall.Context ctx) {
        // strconv.Atoi simulado como funcion libre
        // Para Fase 1 se pueden agregar mas funciones embebidas aqui
        throw new RuntimeException(
            "Error Semantico: Funcion no definida: '" + ctx.name + "' en linea " + ctx.line
        );
    }

    // ========================================================
    // METODOS AUXILIARES PRIVADOS
    // ========================================================

    // Formatea un valor para mostrarlo en consola
    private String formatValue(ValueWrapper val) {
        return switch (val) {
            case StringValue  v -> v.value();
            case IntValue     v -> String.valueOf(v.value());
            case DecimalValue v -> formatDecimal(v.value());
            case BoolValue    v -> String.valueOf(v.value());
            case RuneValue    v -> String.valueOf(v.value());
            case NilValue     v -> "nil";
            default             -> val.toString();
        };
    }

    // Formatea decimales: 1.0 → "1.0", 3.14 → "3.14"
    private String formatDecimal(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.format("%.1f", d);
        }
        return String.valueOf(d);
    }

    // Verifica si dos valores son de tipos compatibles para asignacion
    private boolean typesCompatible(ValueWrapper current, ValueWrapper newVal) {
        if (current.getClass() == newVal.getClass()) return true;
        // Conversion implicita: int → float64
        if (current instanceof DecimalValue && newVal instanceof IntValue) return true;
        return false;
    }

    // Verifica y convierte tipo segun declaracion explicita
    private ValueWrapper checkTypeCompatibility(String declaredType, ValueWrapper val, int line) {
        return switch (declaredType) {
            case "int" -> {
                if (val instanceof IntValue) yield val;
                throw new RuntimeException(
                    "Error Semantico: Se esperaba int pero se recibio " + val.getTypeName() + " en linea " + line
                );
            }
            case "float64" -> {
                if (val instanceof DecimalValue) yield val;
                // Conversion implicita int → float64
                if (val instanceof IntValue iv) yield new DecimalValue((double)iv.value(), iv.line(), iv.column());
                throw new RuntimeException(
                    "Error Semantico: Se esperaba float64 pero se recibio " + val.getTypeName() + " en linea " + line
                );
            }
            case "string" -> {
                if (val instanceof StringValue) yield val;
                throw new RuntimeException(
                    "Error Semantico: Se esperaba string pero se recibio " + val.getTypeName() + " en linea " + line
                );
            }
            case "bool" -> {
                if (val instanceof BoolValue) yield val;
                throw new RuntimeException(
                    "Error Semantico: Se esperaba bool pero se recibio " + val.getTypeName() + " en linea " + line
                );
            }
            case "rune" -> {
                if (val instanceof RuneValue) yield val;
                throw new RuntimeException(
                    "Error Semantico: Se esperaba rune pero se recibio " + val.getTypeName() + " en linea " + line
                );
            }
            default -> val;
        };
    }

    // Valor por defecto segun tipo declarado
    private ValueWrapper getDefaultValue(String type) {
        return switch (type) {
            case "int"     -> new IntValue(0, -1, -1);
            case "float64" -> new DecimalValue(0.0, -1, -1);
            case "string"  -> new StringValue("", -1, -1);
            case "bool"    -> new BoolValue(false, -1, -1);
            case "rune"    -> new RuneValue('\0', -1, -1);
            default        -> new NilValue(-1, -1);
        };
    }

    // Suma directa de dos ValueWrapper (para +=)
    private ValueWrapper addValues(ValueWrapper left, ValueWrapper right, int line) {
        if (left instanceof StringValue l && right instanceof StringValue r) {
            return new StringValue(l.value() + r.value(), l.line(), l.column());
        }
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() + r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue((double)l.value() + r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() + (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() + r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: Operacion invalida para += en linea " + line
            );
        };
    }

    // Resta directa de dos ValueWrapper (para -=)
    private ValueWrapper subValues(ValueWrapper left, ValueWrapper right, int line) {
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() - r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue((double)l.value() - r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() - (double)r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() - r.value(), l.line(), l.column());
            default -> throw new RuntimeException(
                "Error Semantico: Operacion invalida para -= en linea " + line
            );
        };
    }
}