package olc1.golite;

// ============================================================
// IMPORTACIONES
// ============================================================
import java.util.ArrayList;
import java.util.List;

import java_cup.runtime.Symbol;
import olc1.golite.reports.GoliteError;

%%

// ============================================================
// CONFIGURACION DE JFLEX
// ============================================================
%cup
%class Lexer
%public
%line
%column
%8bit

// ELIMINADO: %ignorecase
// GoLite es CASE SENSITIVE, por lo tanto se eliminó %ignorecase.
// Ejemplo: "if" != "IF", "true" != "True"

// ELIMINADO: %unicode
// Se usa %8bit para compatibilidad con los caracteres ASCII del lenguaje.

%{
    // Lista publica de errores lexicos recolectados durante el analisis
    public final List<GoliteError> errors = new ArrayList<>();

    // AGREGADO: Lista publica de tokens reconocidos para el Reporte de Tokens
    // Cada token se guarda como: [lexema, tipo, linea, columna]
    public final List<String[]> tokens = new ArrayList<>();

    // Metodo auxiliar para registrar un token en la lista y retornar el Symbol
    private Symbol token(int type, String typeName, Object value) {
        tokens.add(new String[]{
            String.valueOf(value),
            typeName,
            String.valueOf(yyline),
            String.valueOf(yycolumn)
        });
        return new Symbol(type, yyline, yycolumn, value);
    }
%}

%init{
    yyline = 1;
    yycolumn = 1;
%init}

%eofval{
    return new Symbol(sym.EOF, yyline, yycolumn, yytext());
%eofval}

// ============================================================
// DEFINICION DE PATRONES LEXICOS
// ============================================================

digit       = [0-9]

// MODIFICADO: letter ahora incluye guion bajo _
// Antes: letter = [a-zA-Z]
// Ahora: letter = [a-zA-Z_]
// Razon: Los identificadores validos en GoLite pueden iniciar con _ (ej: _value)
letter      = [a-zA-Z_]

whitespace  = [\ \r\t\f\n]+

// Patrones para strings con secuencias de escape
escape_char = \\ [\"\\nrt]
normal_char = [^\"\\\n\r]
str_lex     = ({normal_char} | {escape_char})*

// AGREGADO: Patron para literales de tipo rune (comilla simple)
// Razon: GoLite tiene tipo rune = 'A', diferente de string = "A"
// Soporta: caracteres normales y secuencias de escape dentro de comilla simple
rune_escape = \\ [\'\\nrt]
rune_char   = [^\'\\\n\r]
rune_lex    = ({rune_char} | {rune_escape})

// AGREGADO: Patrones para comentarios de una linea y multiples lineas
// Razon: GoLite soporta // comentario y /* comentario */
line_comment  = "//"[^\n]*
block_comment = "/*"([^*]|"*"[^/])*"*/"

%%

// ============================================================
// REGLAS LEXICAS
// ============================================================
// IMPORTANTE: El orden importa en JFlex.
// 1. Comentarios primero (para que // no se confunda con division)
// 2. Operadores de 2 caracteres ANTES que los de 1 caracter
// 3. Palabras reservadas ANTES que identificadores
// 4. Numeros antes que simbolos

// ------------------------------------------------------------
// AGREGADO: Comentarios
// Se ignoran completamente, no generan tokens
// ------------------------------------------------------------
{line_comment}  { /* comentario de una linea - ignorar */ }
{block_comment} { /* comentario de multiples lineas - ignorar */ }

// ------------------------------------------------------------
// Numeros
// ------------------------------------------------------------
{digit}+\.{digit}+  { return token(sym.decimal, "float64",  yytext()); }
{digit}+            { return token(sym.integer, "int",      yytext()); }

// ------------------------------------------------------------
// AGREGADO: Literal rune con comilla simple
// Antes no existia soporte para 'A', 'z', '\n', etc.
// ------------------------------------------------------------
\'{rune_lex}\'  { return token(sym.rune, "rune", yytext()); }

// ------------------------------------------------------------
// String con comilla doble (sin cambios, solo se agrego al token reporter)
// ------------------------------------------------------------
\"{str_lex}\"  { return token(sym.string, "string", yytext()); }

// ------------------------------------------------------------
// AGREGADO: Operadores de 2 caracteres
// DEBEN ir antes de los de 1 caracter para que JFlex los reconozca correctamente
// Antes solo existia: +  -  *  /  =
// ------------------------------------------------------------

// Operadores de comparacion
"=="    { return token(sym.eq,    "==", yytext()); }
"!="    { return token(sym.neq,   "!=", yytext()); }
"<="    { return token(sym.lteq,  "<=", yytext()); }
">="    { return token(sym.gteq,  ">=", yytext()); }

// Operadores logicos
"&&"    { return token(sym.and,   "&&", yytext()); }
"||"    { return token(sym.or,    "||", yytext()); }

// Operadores de asignacion compuesta
"+="    { return token(sym.plusAssign,  "+=", yytext()); }
"-="    { return token(sym.minusAssign, "-=", yytext()); }

// AGREGADO: Declaracion con inferencia de tipo (x := valor)
":="    { return token(sym.declare, ":=", yytext()); }

// AGREGADO: Incremento y decremento
"++"    { return token(sym.inc, "++", yytext()); }
"--"    { return token(sym.dec, "--", yytext()); }

// ------------------------------------------------------------
// Operadores de 1 caracter
// ------------------------------------------------------------
"("     { return token(sym.lparen,  "(",  yytext()); }
")"     { return token(sym.rparen,  ")",  yytext()); }
"{"     { return token(sym.lbrace,  "{",  yytext()); }
"}"     { return token(sym.rbrace,  "}",  yytext()); }

// AGREGADO: Corchetes para slices (Fase 2, pero el token debe existir)
"["     { return token(sym.lbracket, "[", yytext()); }
"]"     { return token(sym.rbracket, "]", yytext()); }

"+"     { return token(sym.plus,   "+",  yytext()); }
"-"     { return token(sym.minus,  "-",  yytext()); }
"*"     { return token(sym.times,  "*",  yytext()); }
"/"     { return token(sym.slash,  "/",  yytext()); }

// AGREGADO: Modulo %
// Razon: GoLite requiere operador modulo para int % int
"%"     { return token(sym.mod,    "%",  yytext()); }

// AGREGADO: Operador NOT logico
"!"     { return token(sym.not,    "!",  yytext()); }

// Operadores relacionales de 1 caracter (van despues de <= y >=)
"<"     { return token(sym.lt,     "<",  yytext()); }
">"     { return token(sym.gt,     ">",  yytext()); }

"="     { return token(sym.assign, "=",  yytext()); }
";"     { return token(sym.scol,   ";",  yytext()); }

// AGREGADO: Punto para acceso a atributos (fmt.Println, struct.campo)
"."     { return token(sym.dot,    ".",  yytext()); }

// AGREGADO: Coma para separar argumentos y parametros
","     { return token(sym.comma,  ",",  yytext()); }

// AGREGADO: Dos puntos para switch-case (Fase 2, pero el token debe existir)
":"     { return token(sym.colon,  ":",  yytext()); }

// ------------------------------------------------------------
// PALABRAS RESERVADAS
// DEBEN ir antes del patron de identificadores
// AGREGADAS: func, var, else, for, break, continue, return,
//            nil, int, float64, string, bool, rune, fmt, Println
// NOTA: fmt y Println se manejan como palabras especiales para
//       poder reconocer fmt.Println como funcion embebida
// ------------------------------------------------------------

// Palabras reservadas que ya existian
"if"        { return token(sym.kwIf,       "if",       yytext()); }
"true"      { return token(sym.kwTrue,     "true",     yytext()); }
"false"     { return token(sym.kwFalse,    "false",    yytext()); }

// AGREGADO: Palabras reservadas nuevas para Fase 1
"func"      { return token(sym.kwFunc,     "func",     yytext()); }
"var"       { return token(sym.kwVar,      "var",      yytext()); }
"else"      { return token(sym.kwElse,     "else",     yytext()); }
"for"       { return token(sym.kwFor,      "for",      yytext()); }
"break"     { return token(sym.kwBreak,    "break",    yytext()); }
"continue"  { return token(sym.kwContinue, "continue", yytext()); }
"return"    { return token(sym.kwReturn,   "return",   yytext()); }
"nil"       { return token(sym.kwNil,      "nil",      yytext()); }

// AGREGADO: Tipos de datos como palabras reservadas
"int"       { return token(sym.kwInt,      "int",      yytext()); }
"float64"   { return token(sym.kwFloat64,  "float64",  yytext()); }
"string"    { return token(sym.kwString,   "string",   yytext()); }
"bool"      { return token(sym.kwBool,     "bool",     yytext()); }
"rune"      { return token(sym.kwRune,     "rune",     yytext()); }

// AGREGADO: Identificadores especiales para funciones embebidas
// fmt se reconoce como token especial (luego el parser detecta fmt.Println)
"fmt"       { return token(sym.kwFmt,      "fmt",      yytext()); }

// ELIMINADO: "imprimir" como palabra reservada
// Razon: GoLite usa fmt.Println, no imprimir()
// Antes: "imprimir" { return new Symbol(sym.imprimir, ...) }

// AGREGADO: Para Fase 2 (se agregan ahora para que el lexer no falle)
"switch"    { return token(sym.kwSwitch,   "switch",   yytext()); }
"case"      { return token(sym.kwCase,     "case",     yytext()); }
"default"   { return token(sym.kwDefault,  "default",  yytext()); }
"range"     { return token(sym.kwRange,    "range",    yytext()); }
"struct"    { return token(sym.kwStruct,   "struct",   yytext()); }

// ------------------------------------------------------------
// Identificadores
// MODIFICADO: ahora incluye _ gracias al cambio en letter
// Patron: (letra|_)(letra|digito|_)*
// Antes: {letter}({letter}|{digit})*
// Ahora: {letter}({letter}|{digit})* -- letter ya incluye _
// ------------------------------------------------------------
{letter}({letter}|{digit})*  { return token(sym.id, "id", yytext()); }

// ------------------------------------------------------------
// Ignorar espacios en blanco
// ------------------------------------------------------------
{whitespace}    { /* ignorar espacios, tabs y saltos de linea */ }

// ------------------------------------------------------------
// Manejo de errores lexicos
// Cualquier caracter no reconocido se registra como error
// ------------------------------------------------------------
.   {
        errors.add(new GoliteError(
            "Lexico",
            "Caracter no reconocido: '" + yytext() + "'",
            yyline,
            yycolumn
        ));
    }
