package io.argus.browser.js;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class InterpreterTest {

    private static List<String> run(String source) {
        Interpreter interpreter = new Interpreter();
        interpreter.run(source);
        return interpreter.consoleOutput();
    }

    @Test
    void arithmeticAndConsole() {
        assertEquals(List.of("3"), run("console.log(1 + 2);"));
    }

    @Test
    void stringConcatenation() {
        assertEquals(List.of("ab3"), run("console.log('a' + 'b' + 3);"));
    }

    @Test
    void recursionComputesFactorial() {
        assertEquals(List.of("120"),
                run("function f(n){ return n <= 1 ? 1 : n * f(n - 1); } console.log(f(5));"));
    }

    @Test
    void closuresCaptureState() {
        assertEquals(List.of("1", "2", "3"), run(
                "function make(){ var c = 0; return function(){ c++; return c; }; }"
                        + "var f = make(); console.log(f()); console.log(f()); console.log(f());"));
    }

    @Test
    void arrayMapAndJoin() {
        assertEquals(List.of("2,4,6"),
                run("var a = [1,2,3]; console.log(a.map(function(x){ return x * 2; }).join(','));"));
    }

    @Test
    void objectMethodSeesThis() {
        assertEquals(List.of("Hi Bob"), run(
                "var o = { name: 'Bob', greet: function(){ return 'Hi ' + this.name; } };"
                        + "console.log(o.greet());"));
    }

    @Test
    void classicForLoopSums() {
        assertEquals(List.of("45"),
                run("var s = 0; for (var i = 0; i < 10; i++){ s += i; } console.log(s);"));
    }

    @Test
    void forOfIteratesArray() {
        assertEquals(List.of("a", "b"), run("for (var x of ['a','b']) console.log(x);"));
    }

    @Test
    void whileWithBreak() {
        assertEquals(List.of("0", "1", "2"),
                run("var i = 0; while (true){ if (i >= 3) break; console.log(i); i++; }"));
    }

    @Test
    void typeofHandlesUndeclared() {
        assertEquals(List.of("number", "undefined", "function"), run(
                "console.log(typeof 5); console.log(typeof missing); console.log(typeof console.log);"));
    }

    @Test
    void mathAndParseInt() {
        assertEquals(List.of("3", "10"),
                run("console.log(Math.floor(3.9)); console.log(parseInt('10px'));"));
    }

    @Test
    void stringMethods() {
        assertEquals(List.of("HELLO", "5", "ell"), run(
                "var s = 'hello'; console.log(s.toUpperCase()); console.log(s.length);"
                        + " console.log(s.substring(1,4));"));
    }

    @Test
    void arrayFilterAndLength() {
        assertEquals(List.of("2"), run(
                "var a = [1,2,3,4]; console.log(a.filter(function(x){ return x % 2 === 0; }).length);"));
    }

    @Test
    void infiniteLoopHitsExecutionBudget() {
        // A runaway loop must fail fast rather than hang the caller.
        assertThrows(JsException.class, () -> new Interpreter().run("while (true) {}"));
    }

    @Test
    void runawayRecursionThrowsRangeError() {
        // Unbounded recursion becomes a clean error, not a Java StackOverflowError.
        JsException ex = assertThrows(JsException.class,
                () -> new Interpreter().run("function f(){ return f(); } f();"));
        assertTrue(ex.getMessage().contains("call stack"), ex.getMessage());
    }
}
