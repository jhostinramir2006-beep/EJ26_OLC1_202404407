package olc1.golite.visitor;

import olc1.golite.ast.exp.*;
import olc1.golite.ast.stm.*;

// MODIFICADO: Se agregaron todos los visit() para los nodos nuevos
// Eliminar: visit(Imprimir.Context) fue reemplazado por FmtPrintln
// Agregar: todos los operadores, declaraciones, for, break, continue, return, etc.
public interface Visitor<T> {

    // --------------------------------------------------------
    // Expresiones - Sin cambios
    // --------------------------------------------------------
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

    // --------------------------------------------------------
    // Expresiones - AGREGADAS
    // --------------------------------------------------------
    T visit(Mod.Context ctx);           // %
    T visit(Equal.Context ctx);         // ==
    T visit(NotEqual.Context ctx);      // !=
    T visit(LessThan.Context ctx);      // <
    T visit(GreaterThan.Context ctx);   // >
    T visit(LessEqual.Context ctx);     // <=
    T visit(GreaterEqual.Context ctx);  // >=
    T visit(And.Context ctx);           // &&
    T visit(Or.Context ctx);            // ||
    T visit(Not.Context ctx);           // !
    T visit(NilLiteral.Context ctx);    // nil
    T visit(RuneLiteral.Context ctx);   // 'A'
    T visit(FuncCall.Context ctx);      // funcion(args)

    // --------------------------------------------------------
    // Sentencias - Sin cambios
    // --------------------------------------------------------
    T visit(Assign.Context ctx);
    T visit(Statments.Context ctx);

    // --------------------------------------------------------
    // Sentencias - MODIFICADAS
    // --------------------------------------------------------
    T visit(IfNode.Context ctx);        // ahora incluye else

    // --------------------------------------------------------
    // Sentencias - AGREGADAS
    // --------------------------------------------------------
    T visit(VarDecl.Context ctx);       // var x int = 5
    T visit(VarDeclInfer.Context ctx);  // x := 5
    T visit(PlusAssign.Context ctx);    // x += 5
    T visit(MinusAssign.Context ctx);   // x -= 5
    T visit(Increment.Context ctx);     // x++
    T visit(Decrement.Context ctx);     // x--
    T visit(FmtPrintln.Context ctx);    // fmt.Println(...)
    T visit(ForNode.Context ctx);       // for
    T visit(BreakNode.Context ctx);     // break
    T visit(ContinueNode.Context ctx);  // continue
    T visit(ReturnNode.Context ctx);    // return

    // ELIMINADO: visit(Imprimir.Context ctx)
    // Razon: GoLite usa fmt.Println, no imprimir()
}