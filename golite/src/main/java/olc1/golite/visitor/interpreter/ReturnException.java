package olc1.golite.visitor.interpreter;

import olc1.golite.visitor.interpreter.value.ValueWrapper;

public class ReturnException extends RuntimeException {
    public final ValueWrapper value;

    public ReturnException(ValueWrapper value) {
        this.value = value;
    }
}