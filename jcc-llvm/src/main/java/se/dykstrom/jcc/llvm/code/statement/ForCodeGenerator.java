/*
 * Copyright (C) 2026 Johan Dykstrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.dykstrom.jcc.llvm.code.statement;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FloatLiteral;
import se.dykstrom.jcc.common.ast.ForStatement;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.symbols.Scope;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.llvm.code.Comment;
import se.dykstrom.jcc.llvm.code.FixedLabel;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.LoopStack;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.BranchOperation;
import se.dykstrom.jcc.llvm.operation.LoadOperation;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.ast.IntegerLiteral.ONE;
import static se.dykstrom.jcc.common.ast.IntegerLiteral.ONE_I32;
import static se.dykstrom.jcc.llvm.LlvmOperator.ADD;
import static se.dykstrom.jcc.llvm.LlvmOperator.FADD;
import static se.dykstrom.jcc.llvm.LlvmOperator.FCMP;
import static se.dykstrom.jcc.llvm.LlvmOperator.ICMP;
import static se.dykstrom.jcc.llvm.LlvmUtils.addBranchIfNeeded;
import static se.dykstrom.jcc.llvm.LlvmUtils.addVariable;
import static se.dykstrom.jcc.llvm.LlvmUtils.typeToOperator;

/**
 * Generates LLVM code for FOR statements. The start, end, and step expressions are evaluated
 * once, before the loop, and the end and step values are kept in synthetic locals of the
 * enclosing function. Being allocas in the entry block, they are valid even if the loop body
 * is entered by a jump that bypasses the loop setup.
 * <p>
 * The scope decides where an undefined counter is defined, as in {@link AssignCodeGenerator}.
 * With scope NONE, the counter must already be defined in the symbol table. The expressions
 * must already have the counter's type.
 */
public record ForCodeGenerator(LlvmCodeGenerator cg, LoopStack loopStack, Scope scope) implements LlvmStatementCodeGenerator<ForStatement> {

    public ForCodeGenerator(final LlvmCodeGenerator cg, final LoopStack loopStack, final Scope scope) {
        this.cg = requireNonNull(cg);
        this.loopStack = requireNonNull(loopStack);
        this.scope = requireNonNull(scope);
    }

    @Override
    public void toLlvm(final ForStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var counter = statement.getCounter().getIdentifier();
        if (symbolTable.getCurrentFunction() == null) {
            throw new IllegalStateException("FOR statement outside function");
        }
        if (!symbolTable.contains(counter.name())) {
            addVariable(counter, scope, symbolTable);
        }
        final var type = Opaque.unwrap(counter.type());

        // Create labels
        final var initLabel = new FixedLabel(symbolTable.nextLabelName());
        final var testLabel = new FixedLabel(symbolTable.nextLabelName());
        final var bodyLabel = new FixedLabel(symbolTable.nextLabelName());
        final var incrementLabel = new FixedLabel(symbolTable.nextLabelName());
        final var afterLabel = new FixedLabel(symbolTable.nextLabelName());

        // Create operands for the counter, and the slots holding end and step
        final var opCounter = new TempOperand(symbolTable.mapName(counter), type);
        final var opEndSlot = addSlot(".for." + initLabel.getName() + ".end", type, symbolTable);
        final var opStepSlot = addSlot(".for." + initLabel.getName() + ".step", type, symbolTable);

        // Make sure the basic block before this label ends with a branch operation
        addBranchIfNeeded(lines, initLabel);
        lines.add(new Comment(statement.toString()));

        // Evaluate start, end, and step once, before assigning the counter
        lines.add(initLabel);
        final var opStart = cg.expression(statement.getStart(), lines, symbolTable);
        final var opEnd = cg.expression(statement.getEnd(), lines, symbolTable);
        lines.add(new StoreOperation(opEnd, opEndSlot));
        final var step = (statement.getStep() == null) ? one(type) : statement.getStep();
        final var opStep = cg.expression(step, lines, symbolTable);
        lines.add(new StoreOperation(opStep, opStepSlot));
        lines.add(new StoreOperation(opStart, opCounter));
        lines.add(new BranchOperation(testLabel));

        // Test if the counter has passed the end
        lines.add(testLabel);
        final var opCounterValue = load(opCounter, lines, symbolTable);
        final var opEndValue = load(opEndSlot, lines, symbolTable);
        final var constantUp = isConstantUp(statement.getStep(), symbolTable);
        if (constantUp.isPresent()) {
            final var opCond = compare(opCounterValue, opEndValue, flag(constantUp.get(), statement, type), lines, symbolTable);
            lines.add(new BranchOperation(opCond, bodyLabel, afterLabel));
        } else {
            final var upLabel = new FixedLabel(symbolTable.nextLabelName());
            final var downLabel = new FixedLabel(symbolTable.nextLabelName());
            final var opStepValue = load(opStepSlot, lines, symbolTable);
            final var opZero = new LiteralOperand(type.llvmDefaultValue(), type);
            final var opNegative = compare(opStepValue, opZero, type.isFloat() ? "olt" : "slt", lines, symbolTable);
            lines.add(new BranchOperation(opNegative, downLabel, upLabel));

            lines.add(upLabel);
            final var opUpCond = compare(opCounterValue, opEndValue, flag(true, statement, type), lines, symbolTable);
            lines.add(new BranchOperation(opUpCond, bodyLabel, afterLabel));

            lines.add(downLabel);
            final var opDownCond = compare(opCounterValue, opEndValue, flag(false, statement, type), lines, symbolTable);
            lines.add(new BranchOperation(opDownCond, bodyLabel, afterLabel));
        }

        // Inside loop
        lines.add(bodyLabel);
        loopStack.withLoop(statement.getClass(), afterLabel,
                () -> statement.getStatements().forEach(s -> cg.statement(s, lines, symbolTable)));
        addBranchIfNeeded(lines, incrementLabel);

        // Add step to counter, and jump back to test
        lines.add(incrementLabel);
        final var opOldCounter = load(opCounter, lines, symbolTable);
        final var opStepIncrement = load(opStepSlot, lines, symbolTable);
        final var opNewCounter = new TempOperand(symbolTable.nextTempName(), type);
        lines.add(new BinaryOperation(opNewCounter, typeToOperator(type, FADD, ADD), opOldCounter, opStepIncrement));
        lines.add(new StoreOperation(opNewCounter, opCounter));
        lines.add(new BranchOperation(testLabel));

        // After loop
        lines.add(new Comment("END FOR"));
        lines.add(afterLabel);
    }

    /**
     * Adds a synthetic local to the symbol table, and returns an operand that points to it.
     * The enclosing function allocates stack space for it in its entry block.
     */
    private static TempOperand addSlot(final String name, final Type type, final SymbolTable symbolTable) {
        final var identifier = new Identifier(name, type);
        symbolTable.addVariable(identifier, null);
        return new TempOperand(symbolTable.mapName(identifier), type);
    }

    private static TempOperand load(final TempOperand source, final List<Line> lines, final SymbolTable symbolTable) {
        final var opResult = new TempOperand(symbolTable.nextTempName(), source.type());
        lines.add(new LoadOperation(opResult, source));
        return opResult;
    }

    private static TempOperand compare(final LlvmOperand left,
                                       final LlvmOperand right,
                                       final String flag,
                                       final List<Line> lines,
                                       final SymbolTable symbolTable) {
        final var opResult = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
        final var operator = typeToOperator(left.type(), FCMP, ICMP);
        lines.add(new BinaryOperation(opResult, operator, left, right, flag));
        return opResult;
    }

    /**
     * Returns the compare flag that is true while the loop should continue.
     */
    private static String flag(final boolean up, final ForStatement statement, final Type type) {
        final var inclusive = statement.isInclusive();
        if (type.isFloat()) {
            return up ? (inclusive ? "ole" : "olt") : (inclusive ? "oge" : "ogt");
        } else {
            return up ? (inclusive ? "sle" : "slt") : (inclusive ? "sge" : "sgt");
        }
    }

    /**
     * Returns an optional valued true if the step is a non-negative constant, false if it is a negative constant,
     * and empty if the step is not a constant. A missing step is the constant 1.
     */
    private Optional<Boolean> isConstantUp(final Expression step, final SymbolTable symbolTable) {
        if (step == null) {
            return Optional.of(true);
        }
        return switch (cg.optimizer().expressionOptimizer().expression(step, symbolTable)) {
            case IntegerLiteral literal -> Optional.of(literal.asLong() >= 0);
            case FloatLiteral literal -> Optional.of(literal.asDouble() >= 0);
            default -> Optional.empty();
        };
    }

    private static Expression one(final Type type) {
        return switch (type) {
            case I64 ignored -> ONE;
            case I32 ignored -> ONE_I32;
            case F64 ignored -> new FloatLiteral(0, 0, "1.0", F64.INSTANCE);
            default -> throw new IllegalArgumentException("unsupported counter type: " + type.getName());
        };
    }
}
