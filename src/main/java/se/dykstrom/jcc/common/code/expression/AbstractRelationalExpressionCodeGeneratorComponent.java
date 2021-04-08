/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.common.code.expression;

import se.dykstrom.jcc.common.assembly.base.*;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_STRCMP;

public abstract class AbstractRelationalExpressionCodeGeneratorComponent<E extends BinaryExpression>
        extends AbstractExpressionCodeGeneratorComponent<E, TypeManager, AbstractCodeGenerator> {

    private static final Label LABEL_ANON_FWD = new FixedLabel("@f");
    private static final Label LABEL_ANON_TARGET = new FixedLabel("@@");

    protected AbstractRelationalExpressionCodeGeneratorComponent(Context context) {
        super(context);
    }

    /**
     * Generates code for the relational expression denoted by {@code expression},
     * storing the result in {@code leftLocation}. The functions {@code branchFunction}
     * and {@code floatBranchFunction} should be functions that take a label as input,
     * and return a conditional branch instruction to that label.
     *
     * As an example, for an equal expression (==) the given functions should both generate a
     * JE instruction (jump if equal). For a less than expression (<) the {@code branchFunction}
     * should generate a JL instruction. The {@code floatBranchFunction} should generate a
     * JB instruction, that is used after comparing floats.
     */
    protected List<Line> relationalExpression(BinaryExpression expression,
                                              StorageLocation leftLocation,
                                              Function<Label, Instruction> branchFunction,
                                              Function<Label, Instruction> floatBranchFunction) {
        Type leftType = types.getType(expression.getLeft());
        Type rightType = types.getType(expression.getRight());

        if (leftType == Str.INSTANCE) {
            return relationalStringExpression(expression, leftLocation, branchFunction);
        } else if (leftType == F64.INSTANCE || rightType == F64.INSTANCE) {
            return relationalFloatExpression(expression, leftLocation, floatBranchFunction);
        } else {
            return relationalIntegerExpression(expression, leftLocation, branchFunction);
        }
    }

    /**
     * Generates code for comparing one or more floating point values.
     */
    private List<Line> relationalFloatExpression(BinaryExpression expression,
                                                 StorageLocation leftLocation,
                                                 Function<Label, Instruction> branchFunction) {
        CodeContainer cc = new CodeContainer();

        try (StorageLocation leftFloatLocation = storageFactory.allocateNonVolatile(F64.INSTANCE);
             StorageLocation rightFloatLocation = storageFactory.allocateNonVolatile(F64.INSTANCE)) {
            // Generate code for left sub expression, and store result in leftFloatLocation
            codeGenerator.expression(expression.getLeft(), leftFloatLocation);
            // Generate code for right sub expression, and store result in rightFloatLocation
            codeGenerator.expression(expression.getRight(), rightFloatLocation);

            // Generate a unique label name
            Label afterCmpLabel = new Label(codeGenerator.uniqifyLabelName("after_cmp_"));

            // Generate code for comparing sub expressions, and store result in leftLocation
            cc.add(getComment(expression));
            leftFloatLocation.compareThisWithLoc(rightFloatLocation, cc);
            cc.add(branchFunction.apply(LABEL_ANON_FWD));
            leftLocation.moveImmToThis("0", cc); // Boolean FALSE
            cc.add(new Jmp(afterCmpLabel));
            cc.add(LABEL_ANON_TARGET);
            leftLocation.moveImmToThis("-1", cc); // Boolean TRUE
            cc.add(afterCmpLabel);
        }

        return cc.lines();
    }

    /**
     * Generates code for comparing two integer values.
     */
    private List<Line> relationalIntegerExpression(BinaryExpression expression,
                                                   StorageLocation leftLocation,
                                                   Function<Label, Instruction> branchFunction) {
        CodeContainer cc = new CodeContainer();

        // Generate code for left sub expression, and store result in leftLocation
        codeGenerator.expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            codeGenerator.expression(expression.getRight(), rightLocation);
            // Generate a unique label name
            Label afterCmpLabel = new Label(codeGenerator.uniqifyLabelName("after_cmp_"));

            // Generate code for comparing sub expressions, and store result in leftLocation
            cc.add(getComment(expression));
            leftLocation.compareThisWithLoc(rightLocation, cc);
            cc.add(branchFunction.apply(LABEL_ANON_FWD));
            leftLocation.moveImmToThis("0", cc); // Boolean FALSE
            cc.add(new Jmp(afterCmpLabel));
            cc.add(LABEL_ANON_TARGET);
            leftLocation.moveImmToThis("-1", cc); // Boolean TRUE
            cc.add(afterCmpLabel);
        }

        return cc.lines();
    }

    /**
     * Generates code for comparing two string values.
     */
    private List<Line> relationalStringExpression(BinaryExpression expression,
                                                  StorageLocation leftLocation,
                                                  Function<Label, Instruction> branchFunction) {
        CodeContainer cc = new CodeContainer();

        // Evaluate expressions, and call strcmp, ending up with the result in RAX
        codeGenerator.addFunctionCall(FUN_STRCMP, getComment(expression), asList(expression.getLeft(), expression.getRight()), leftLocation);

        // Generate a unique label name
        Label afterCmpLabel = new Label(codeGenerator.uniqifyLabelName("after_cmp_"));

        // Generate code for comparing the result of calling strcmp with 0, and store result in leftLocation
        leftLocation.compareThisWithImm("0", cc);
        cc.add(branchFunction.apply(LABEL_ANON_FWD));
        leftLocation.moveImmToThis("0", cc); // Boolean FALSE
        cc.add(new Jmp(afterCmpLabel));
        cc.add(LABEL_ANON_TARGET);
        leftLocation.moveImmToThis("-1", cc); // Boolean TRUE
        cc.add(afterCmpLabel);

        return cc.lines();
    }
}
