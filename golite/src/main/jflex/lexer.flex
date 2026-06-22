package olc1.golite;

// Importaciones necesarias
import java.util.ArrayList;
import java.util.List;

import java_cup.runtime.Symbol;

import olc1.golite.reports.GoliteError;

%%

// Configuración de JFLEX
%cup //Indicamos que vamos a usar CUP
// Nombre de la clase del lexer
%class Lexer 
%public // Paquete del lexer
%line // conteo de lienas
%column // conteo de columnas
%unicode
//%8bit   recibir caracteres en formato UTF-8
// %debug // Habilitar modo debug para ver el proceso de tokenización
%ignorecase // ignorar mayusculas y minusculas
//%unicode

%{
    // private Symbol symbol(int type) {
    //     return new Symbol(type, yyline, yycolumn);
    // }

    // private Symbol symbol(int type, Object value) {
    //     return new Symbol(type, yyline, yycolumn, value);
    // }

    public final List<GoliteError> errors = new ArrayList<>();
%}

%init{
    yyline = 1;
    yycolumn = 1;
%init}

%eofval{
    return new Symbol(sym.EOF, yyline, yycolumn, yytext());
%eofval}

// Definición de patrones léxicos
digit = [0-9]
letter = [a-zA-Z_]
whitespace = [\ \r\t\f\n]+
escape_char = \\ [\"\\nrt]
normal_char = [^\"\\\n\r]
str_lex = ({normal_char} | {escape_char})*

%%

// Comentarios
"//"[^\n\r]* {
    /* ignorar */
}

"/*"([^*]|\*+[^*/])*\*+"/" {
    /* ignorar */
}

// Numbers
{digit}+\.{digit}+  { return new Symbol(sym.decimal, yyline + 1, yycolumn + 1, yytext()); }
{digit}+            { return new Symbol(sym.integer, yyline + 1, yycolumn + 1, yytext()); }

// Symbols
"("     { return new Symbol(sym.lparen, yyline, yycolumn, yytext()); }
")"     { return new Symbol(sym.rparen, yyline, yycolumn, yytext()); }

"++"    { return new Symbol(sym.inc, yyline, yycolumn, yytext()); }
"--"    { return new Symbol(sym.dec, yyline, yycolumn, yytext()); }

"+="    { return new Symbol(sym.plusAssign, yyline, yycolumn, yytext()); }
"-="    { return new Symbol(sym.minusAssign, yyline, yycolumn, yytext()); }

":="    { return new Symbol(sym.declare, yyline, yycolumn, yytext()); }

"=="    { return new Symbol(sym.eq, yyline, yycolumn, yytext()); }
"!="    { return new Symbol(sym.neq, yyline, yycolumn, yytext()); }

">="    { return new Symbol(sym.ge, yyline, yycolumn, yytext()); }
"<="    { return new Symbol(sym.le, yyline, yycolumn, yytext()); }

"&&"    { return new Symbol(sym.and, yyline, yycolumn, yytext()); }
"||"    { return new Symbol(sym.or, yyline, yycolumn, yytext()); }

"+"     { return new Symbol(sym.plus, yyline, yycolumn, yytext()); }
"-"     { return new Symbol(sym.minus, yyline, yycolumn, yytext()); }

"*"     { return new Symbol(sym.times, yyline, yycolumn, yytext()); }
"/"     { return new Symbol(sym.slash, yyline, yycolumn, yytext()); }

">"     { return new Symbol(sym.gt, yyline, yycolumn, yytext()); }
"<"     { return new Symbol(sym.lt, yyline, yycolumn, yytext()); }

"="     { return new Symbol(sym.assign, yyline, yycolumn, yytext()); }

"!"     { return new Symbol(sym.not, yyline, yycolumn, yytext()); }

"%"     { return new Symbol(sym.mod, yyline, yycolumn, yytext()); }

"."     { return new Symbol(sym.dot, yyline, yycolumn, yytext()); }
","     { return new Symbol(sym.comma, yyline, yycolumn, yytext()); }

";"     { return new Symbol(sym.scol, yyline, yycolumn, yytext()); }

"{"     { return new Symbol(sym.lbrace, yyline, yycolumn, yytext()); }
"}"     { return new Symbol(sym.rbrace, yyline, yycolumn, yytext()); }
"["     { return new Symbol(sym.lbracket, yyline, yycolumn, yytext()); }
"]"     { return new Symbol(sym.rbracket, yyline, yycolumn, yytext()); }
// Key Words

"for"       { return new Symbol(sym.kwFor, yyline, yycolumn, yytext()); }
"func"      { return new Symbol(sym.kwFunc, yyline, yycolumn, yytext()); }

"if"        { return new Symbol(sym.kwIf, yyline, yycolumn, yytext()); }
"else"      { return new Symbol(sym.kwElse, yyline, yycolumn, yytext()); }

"break"     { return new Symbol(sym.kwBreak, yyline, yycolumn, yytext()); }
"continue"  { return new Symbol(sym.kwContinue, yyline, yycolumn, yytext()); }

"var"       { return new Symbol(sym.kwVar, yyline, yycolumn, yytext()); }

"int"       { return new Symbol(sym.kwInt, yyline, yycolumn, yytext()); }
"float64"   { return new Symbol(sym.kwFloat64, yyline, yycolumn, yytext()); }
"string"    { return new Symbol(sym.kwString, yyline, yycolumn, yytext()); }
"bool"      { return new Symbol(sym.kwBool, yyline, yycolumn, yytext()); }

"true"      { return new Symbol(sym.kwTrue, yyline, yycolumn, yytext()); }
"false"     { return new Symbol(sym.kwFalse, yyline, yycolumn, yytext()); }

"fmt"       { return new Symbol(sym.kwFmt, yyline, yycolumn, yytext()); }

"imprimir"  { return new Symbol(sym.imprimir, yyline, yycolumn, yytext()); }
"rune"      { return new Symbol(sym.kwRune, yyline, yycolumn, yytext()); }
"range"     { return new Symbol(sym.kwRange, yyline, yycolumn, yytext()); }
"reflect"   { return new Symbol(sym.kwReflect, yyline, yycolumn, yytext()); }
"TypeOf"    { return new Symbol(sym.kwTypeOf, yyline, yycolumn, yytext()); }

"strconv"   { return new Symbol(sym.kwStrconv, yyline, yycolumn, yytext()); }
"Atoi"      { return new Symbol(sym.kwAtoi, yyline, yycolumn, yytext()); }
"ParseFloat"  { return new Symbol(sym.kwParseFloat, yyline, yycolumn, yytext()); }

"nil"       { return new Symbol(sym.kwNil, yyline, yycolumn, yytext()); }
"switch"    { return new Symbol(sym.kwSwitch, yyline, yycolumn, yytext()); }
"case"      { return new Symbol(sym.kwCase, yyline, yycolumn, yytext()); }
"default"   { return new Symbol(sym.kwDefault, yyline, yycolumn, yytext()); }
":"         { return new Symbol(sym.colon, yyline, yycolumn, yytext()); }
"return"    { return new Symbol(sym.kwReturn, yyline, yycolumn, yytext()); }
"slices"    { return new Symbol(sym.kwSlices, yyline, yycolumn, yytext()); }
"Index"     { return new Symbol(sym.kwIndex, yyline, yycolumn, yytext()); }

"strings"   { return new Symbol(sym.kwStrings, yyline, yycolumn, yytext()); }
"Join"      { return new Symbol(sym.kwJoin, yyline, yycolumn, yytext()); }

"len"       { return new Symbol(sym.kwLen, yyline, yycolumn, yytext()); }
"append"    { return new Symbol(sym.kwAppend, yyline, yycolumn, yytext()); }
\'([^\'\\\n\r]|\\[nrt\'\\])\'
{
    return new Symbol(sym.rune_literal, yyline, yycolumn, yytext());
}
// ID - String
{letter}({letter}|{digit})* {
    return new Symbol(sym.id, yyline, yycolumn, yytext());
}
\"{str_lex}\"               { return new Symbol(sym.string, yyline, yycolumn, yytext()); }

// Ignorar
{whitespace}    {/* pass */}

// Error
.   { errors.add(new GoliteError("Lexico", "Caracter no reconocido: " + yytext(), yyline, yycolumn)); }
