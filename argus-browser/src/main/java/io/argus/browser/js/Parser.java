package io.argus.browser.js;

import io.argus.browser.js.Ast.Block;
import io.argus.browser.js.Ast.Declarator;
import io.argus.browser.js.Ast.Expr;
import io.argus.browser.js.Ast.Program;
import io.argus.browser.js.Ast.Property;
import io.argus.browser.js.Ast.Stmt;
import java.util.ArrayList;
import java.util.List;

/**
 * A recursive-descent parser for the JavaScript subset. It produces an {@link Ast.Program} using a
 * conventional precedence ladder (assignment → conditional → logical → equality → relational →
 * additive → multiplicative → unary → postfix/call/member → primary). Semicolons are optional, which
 * approximates automatic semicolon insertion well enough for scripts found in ordinary pages.
 */
public final class Parser {

    private final List<JsToken> tokens;
    private int index;

    public Parser(String source) {
        this.tokens = new Lexer(source).tokenize();
    }

    public Program parseProgram() {
        List<Stmt> body = new ArrayList<>();
        while (!atEnd()) {
            body.add(statement());
        }
        return new Program(body);
    }

    // ---- Statements -----------------------------------------------------------------------------

    private Stmt statement() {
        if (matchPunct(";")) {
            return new Ast.Block(List.of());
        }
        if (checkPunct("{")) {
            return block();
        }
        if (isKeyword("var") || isKeyword("let") || isKeyword("const")) {
            Stmt decl = varDeclaration();
            consumeSemicolon();
            return decl;
        }
        if (isKeyword("if")) {
            return ifStatement();
        }
        if (isKeyword("while")) {
            return whileStatement();
        }
        if (isKeyword("for")) {
            return forStatement();
        }
        if (isKeyword("function")) {
            return functionDeclaration();
        }
        if (isKeyword("return")) {
            advance();
            Expr arg = (checkPunct(";") || checkPunct("}") || atEnd()) ? null : expression();
            consumeSemicolon();
            return new Ast.Return(arg);
        }
        if (isKeyword("break")) {
            advance();
            consumeSemicolon();
            return new Ast.Break();
        }
        if (isKeyword("continue")) {
            advance();
            consumeSemicolon();
            return new Ast.Continue();
        }
        Expr expr = expression();
        consumeSemicolon();
        return new Ast.ExprStmt(expr);
    }

    private Block block() {
        expectPunct("{");
        List<Stmt> statements = new ArrayList<>();
        while (!checkPunct("}") && !atEnd()) {
            statements.add(statement());
        }
        expectPunct("}");
        return new Block(statements);
    }

    private Stmt varDeclaration() {
        String kind = advance().text();
        List<Declarator> declarators = new ArrayList<>();
        do {
            String name = identifierName();
            Expr init = matchPunct("=") ? assignment() : null;
            declarators.add(new Declarator(name, init));
        } while (matchPunct(","));
        return new Ast.VarDecl(kind, declarators);
    }

    private Stmt ifStatement() {
        advance();
        expectPunct("(");
        Expr test = expression();
        expectPunct(")");
        Stmt consequent = statement();
        Stmt alternate = null;
        if (isKeyword("else")) {
            advance();
            alternate = statement();
        }
        return new Ast.If(test, consequent, alternate);
    }

    private Stmt whileStatement() {
        advance();
        expectPunct("(");
        Expr test = expression();
        expectPunct(")");
        return new Ast.While(test, statement());
    }

    private Stmt forStatement() {
        advance();
        expectPunct("(");
        if (isKeyword("var") || isKeyword("let") || isKeyword("const")) {
            String kind = advance().text();
            String name = identifierName();
            if (isKeyword("of") || isKeyword("in")) {
                boolean ofLoop = advance().text().equals("of");
                Expr object = expression();
                expectPunct(")");
                return new Ast.ForEach(kind, name, object, statement(), ofLoop);
            }
            List<Declarator> declarators = new ArrayList<>();
            Expr init = matchPunct("=") ? assignment() : null;
            declarators.add(new Declarator(name, init));
            while (matchPunct(",")) {
                String n = identifierName();
                Expr i = matchPunct("=") ? assignment() : null;
                declarators.add(new Declarator(n, i));
            }
            expectPunct(";");
            return finishClassicFor(new Ast.VarDecl(kind, declarators));
        }
        Stmt init = checkPunct(";") ? null : new Ast.ExprStmt(expression());
        expectPunct(";");
        return finishClassicFor(init);
    }

    private Stmt finishClassicFor(Stmt init) {
        Expr test = checkPunct(";") ? null : expression();
        expectPunct(";");
        Expr update = checkPunct(")") ? null : expression();
        expectPunct(")");
        return new Ast.For(init, test, update, statement());
    }

    private Stmt functionDeclaration() {
        advance();
        String name = identifierName();
        List<String> params = parameterList();
        return new Ast.FunctionDecl(name, params, block());
    }

    // ---- Expressions ----------------------------------------------------------------------------

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr left = conditional();
        if (checkPunct("=") || checkPunct("+=") || checkPunct("-=") || checkPunct("*=")
                || checkPunct("/=") || checkPunct("%=")) {
            String op = advance().text();
            Expr value = assignment();
            return new Ast.Assign(left, op, value);
        }
        return left;
    }

    private Expr conditional() {
        Expr test = logicalOr();
        if (matchPunct("?")) {
            Expr consequent = assignment();
            expectPunct(":");
            Expr alternate = assignment();
            return new Ast.Conditional(test, consequent, alternate);
        }
        return test;
    }

    private Expr logicalOr() {
        Expr left = logicalAnd();
        while (checkPunct("||")) {
            advance();
            left = new Ast.Logical("||", left, logicalAnd());
        }
        return left;
    }

    private Expr logicalAnd() {
        Expr left = equality();
        while (checkPunct("&&")) {
            advance();
            left = new Ast.Logical("&&", left, equality());
        }
        return left;
    }

    private Expr equality() {
        Expr left = relational();
        while (checkPunct("==") || checkPunct("!=") || checkPunct("===") || checkPunct("!==")) {
            String op = advance().text();
            left = new Ast.Binary(op, left, relational());
        }
        return left;
    }

    private Expr relational() {
        Expr left = additive();
        while (checkPunct("<") || checkPunct(">") || checkPunct("<=") || checkPunct(">=")) {
            String op = advance().text();
            left = new Ast.Binary(op, left, additive());
        }
        return left;
    }

    private Expr additive() {
        Expr left = multiplicative();
        while (checkPunct("+") || checkPunct("-")) {
            String op = advance().text();
            left = new Ast.Binary(op, left, multiplicative());
        }
        return left;
    }

    private Expr multiplicative() {
        Expr left = unary();
        while (checkPunct("*") || checkPunct("/") || checkPunct("%")) {
            String op = advance().text();
            left = new Ast.Binary(op, left, unary());
        }
        return left;
    }

    private Expr unary() {
        if (checkPunct("!") || checkPunct("-") || checkPunct("+") || isKeyword("typeof")) {
            String op = advance().text();
            return new Ast.Unary(op, unary());
        }
        if (checkPunct("++") || checkPunct("--")) {
            String op = advance().text();
            return new Ast.Update(op, unary(), true);
        }
        return postfix();
    }

    private Expr postfix() {
        Expr expr = callAndMember();
        if (checkPunct("++") || checkPunct("--")) {
            String op = advance().text();
            return new Ast.Update(op, expr, false);
        }
        return expr;
    }

    private Expr callAndMember() {
        Expr expr = primary();
        while (true) {
            if (matchPunct(".")) {
                expr = new Ast.Member(expr, new Ast.Identifier(identifierName()), false);
            } else if (matchPunct("[")) {
                Expr property = expression();
                expectPunct("]");
                expr = new Ast.Member(expr, property, true);
            } else if (checkPunct("(")) {
                expr = new Ast.Call(expr, argumentList());
            } else {
                return expr;
            }
        }
    }

    private Expr primary() {
        JsToken t = peek();
        switch (t.type()) {
            case NUMBER -> {
                advance();
                return new Ast.NumberLit(Double.parseDouble(t.text()));
            }
            case STRING -> {
                advance();
                return new Ast.StringLit(t.text());
            }
            case IDENTIFIER -> {
                advance();
                return new Ast.Identifier(t.text());
            }
            case KEYWORD -> {
                return keywordPrimary();
            }
            default -> {
                return punctuatorPrimary();
            }
        }
    }

    private Expr keywordPrimary() {
        JsToken t = peek();
        switch (t.text()) {
            case "true" -> {
                advance();
                return new Ast.BoolLit(true);
            }
            case "false" -> {
                advance();
                return new Ast.BoolLit(false);
            }
            case "null" -> {
                advance();
                return new Ast.NullLit();
            }
            case "undefined" -> {
                advance();
                return new Ast.UndefinedLit();
            }
            case "this" -> {
                advance();
                return new Ast.ThisExpr();
            }
            case "function" -> {
                advance();
                String name = peek().type() == JsToken.Type.IDENTIFIER ? advance().text() : null;
                List<String> params = parameterList();
                return new Ast.FunctionExpr(name, params, block());
            }
            case "new" -> {
                advance();
                Expr callee = primary();
                while (matchPunct(".")) {
                    callee = new Ast.Member(callee, new Ast.Identifier(identifierName()), false);
                }
                List<Expr> args = checkPunct("(") ? argumentList() : List.of();
                return new Ast.NewExpr(callee, args);
            }
            default -> throw new JsException("Unexpected keyword '" + t.text() + "'");
        }
    }

    private Expr punctuatorPrimary() {
        if (matchPunct("(")) {
            Expr expr = expression();
            expectPunct(")");
            return expr;
        }
        if (matchPunct("[")) {
            List<Expr> elements = new ArrayList<>();
            if (!checkPunct("]")) {
                do {
                    if (checkPunct("]")) {
                        break;
                    }
                    elements.add(assignment());
                } while (matchPunct(","));
            }
            expectPunct("]");
            return new Ast.ArrayLit(elements);
        }
        if (matchPunct("{")) {
            List<Property> properties = new ArrayList<>();
            if (!checkPunct("}")) {
                do {
                    if (checkPunct("}")) {
                        break;
                    }
                    String key = advance().text();
                    expectPunct(":");
                    properties.add(new Property(key, assignment()));
                } while (matchPunct(","));
            }
            expectPunct("}");
            return new Ast.ObjectLit(properties);
        }
        throw new JsException("Unexpected token '" + peek().text() + "'");
    }

    private List<String> parameterList() {
        expectPunct("(");
        List<String> params = new ArrayList<>();
        if (!checkPunct(")")) {
            do {
                params.add(identifierName());
            } while (matchPunct(","));
        }
        expectPunct(")");
        return params;
    }

    private List<Expr> argumentList() {
        expectPunct("(");
        List<Expr> args = new ArrayList<>();
        if (!checkPunct(")")) {
            do {
                args.add(assignment());
            } while (matchPunct(","));
        }
        expectPunct(")");
        return args;
    }

    // ---- Token helpers --------------------------------------------------------------------------

    private JsToken peek() {
        return tokens.get(index);
    }

    private JsToken advance() {
        JsToken t = tokens.get(index);
        if (index < tokens.size() - 1) {
            index++;
        }
        return t;
    }

    private boolean atEnd() {
        return peek().type() == JsToken.Type.EOF;
    }

    private boolean checkPunct(String s) {
        return peek().is(JsToken.Type.PUNCTUATOR, s);
    }

    private boolean matchPunct(String s) {
        if (checkPunct(s)) {
            advance();
            return true;
        }
        return false;
    }

    private void expectPunct(String s) {
        if (!matchPunct(s)) {
            throw new JsException("Expected '" + s + "' but found '" + peek().text() + "'");
        }
    }

    private boolean isKeyword(String s) {
        return peek().is(JsToken.Type.KEYWORD, s);
    }

    private String identifierName() {
        JsToken t = peek();
        if (t.type() == JsToken.Type.IDENTIFIER || t.type() == JsToken.Type.KEYWORD) {
            advance();
            return t.text();
        }
        throw new JsException("Expected an identifier but found '" + t.text() + "'");
    }

    private void consumeSemicolon() {
        matchPunct(";"); // optional (automatic semicolon insertion approximation)
    }
}
