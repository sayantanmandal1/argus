package io.argus.browser.js;

import java.util.List;

/** A user-defined JavaScript function: its parameters, body, and the scope it closed over. */
public final class UserFunction implements JsCallable {

    private final String name;
    private final List<String> params;
    private final Ast.Block body;
    private final Environment closure;

    public UserFunction(String name, List<String> params, Ast.Block body, Environment closure) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
    }

    public String name() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, Object thisValue, List<Object> arguments) {
        Environment env = new Environment(closure);
        env.define("this", thisValue);
        env.define("arguments", new JsArray(arguments));
        for (int i = 0; i < params.size(); i++) {
            env.define(params.get(i), i < arguments.size() ? arguments.get(i) : Undefined.VALUE);
        }
        try {
            interpreter.executeStatements(body.statements(), env);
        } catch (Interpreter.ReturnSignal signal) {
            return signal.value();
        }
        return Undefined.VALUE;
    }
}
