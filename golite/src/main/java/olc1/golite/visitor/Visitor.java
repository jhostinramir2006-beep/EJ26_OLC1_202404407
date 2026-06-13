package olc1.golite.visitor;

import olc1.golite.ast.exp.*;
import olc1.golite.ast.stm.*;

public interface Visitor<T> {
    T visit(Integers.Context ctx);
    T visit(Decimal.Context ctx);
    T visit(Paren.Context ctx);
    T visit(Add.Context ctx);
    T visit(Sub.Context ctx);
    T visit(Mul.Context ctx);
    T visit(Div.Context ctx);
    T visit(Negate.Context ctx);
    T visit(BoolLiteral.Context ctx);
    T visit(StringLiteral.Context ctx);
    T visit(VarRef.Context ctx);
    T visit(Imprimir.Context ctx);
    T visit(Assign.Context ctx);
    T visit(IfNode.Context ctx);
    T visit(Statments.Context ctx);
    T visit(GreaterThan.Context ctx);
    T visit(LessThan.Context ctx);
    T visit(Equals.Context ctx);
    T visit(NotEquals.Context ctx);
    T visit(GreaterEqual.Context ctx);
    T visit(LessEqual.Context ctx);
    T visit(And.Context ctx);
    T visit(Or.Context ctx);
    T visit(Not.Context ctx);
    T visit(Mod.Context ctx);
    T visit(PlusAssign.Context ctx);
    T visit(MinusAssign.Context ctx);
    T visit(ForNode.Context ctx);
    T visit(Increment.Context ctx);
    T visit(Decrement.Context ctx);
    T visit(FmtPrintln.Context ctx);
    T visit(BreakNode.Context ctx);
    T visit(ContinueNode.Context ctx);
    T visit(VarDeclInfer.Context ctx);
    T visit(VarDecl.Context ctx);
}
