package io.argus.browser.js;

import java.util.List;

/**
 * The abstract syntax tree for the supported JavaScript subset. Kept as small records grouped under
 * {@link Expr} and {@link Stmt} so the {@link Parser} builds them and the {@link Interpreter} walks
 * them with pattern matching. The interfaces are intentionally not sealed to keep the node set easy
 * to extend.
 */
public interface Ast {

    interface Expr extends Ast {
    }

    interface Stmt extends Ast {
    }

    // ---- Expressions ----------------------------------------------------------------------------

    record NumberLit(double value) implements Expr {
    }

    record StringLit(String value) implements Expr {
    }

    record BoolLit(boolean value) implements Expr {
    }

    record NullLit() implements Expr {
    }

    record UndefinedLit() implements Expr {
    }

    record Identifier(String name) implements Expr {
    }

    record ThisExpr() implements Expr {
    }

    record ArrayLit(List<Expr> elements) implements Expr {
    }

    record Property(String key, Expr value) {
    }

    record ObjectLit(List<Property> properties) implements Expr {
    }

    record Unary(String op, Expr operand) implements Expr {
    }

    record Update(String op, Expr target, boolean prefix) implements Expr {
    }

    record Binary(String op, Expr left, Expr right) implements Expr {
    }

    record Logical(String op, Expr left, Expr right) implements Expr {
    }

    record Assign(Expr target, String op, Expr value) implements Expr {
    }

    record Conditional(Expr test, Expr consequent, Expr alternate) implements Expr {
    }

    record Call(Expr callee, List<Expr> arguments) implements Expr {
    }

    record NewExpr(Expr callee, List<Expr> arguments) implements Expr {
    }

    /** Member access: {@code object.name} (computed=false) or {@code object[expr]} (computed=true). */
    record Member(Expr object, Expr property, boolean computed) implements Expr {
    }

    record FunctionExpr(String name, List<String> params, Block body) implements Expr {
    }

    // ---- Statements -----------------------------------------------------------------------------

    record ExprStmt(Expr expression) implements Stmt {
    }

    record Declarator(String name, Expr init) {
    }

    record VarDecl(String kind, List<Declarator> declarations) implements Stmt {
    }

    record Block(List<Stmt> statements) implements Stmt {
    }

    record If(Expr test, Stmt consequent, Stmt alternate) implements Stmt {
    }

    record While(Expr test, Stmt body) implements Stmt {
    }

    record For(Stmt init, Expr test, Expr update, Stmt body) implements Stmt {
    }

    record ForEach(String kind, String name, Expr object, Stmt body, boolean ofLoop) implements Stmt {
    }

    record Return(Expr argument) implements Stmt {
    }

    record FunctionDecl(String name, List<String> params, Block body) implements Stmt {
    }

    record Break() implements Stmt {
    }

    record Continue() implements Stmt {
    }

    record Program(List<Stmt> body) implements Ast {
    }
}
