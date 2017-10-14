package se.dykstrom.jcc.assembunny.ast;

import java.util.Objects;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents an expression referencing an Assembunny register.
 *
 * @author Johan Dykstrom
 */
public class RegisterExpression extends Expression implements TypedExpression {

    private final AssembunnyRegister register;

    public RegisterExpression(int line, int column, AssembunnyRegister register) {
        super(line, column);
        this.register = register;
    }

    public AssembunnyRegister getRegister() {
        return register;
    }

    @Override
    public String toString() {
        return register.toString().toLowerCase();
    }

    @Override
    public Type getType() {
        return I64.INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterExpression that = (RegisterExpression) o;
        return Objects.equals(this.register, that.register);
    }

    @Override
    public int hashCode() {
        return Objects.hash(register);
    }
}
