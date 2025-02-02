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

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;
import static se.dykstrom.jcc.common.functions.MemoryManagementUtils.allocatesDynamicMemory;

/**
 * GC extension of {@link AddCodeGenerator} that also handles string addition.
 */
public class GcAddCodeGenerator extends AddCodeGenerator {

    public GcAddCodeGenerator(final AsmCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(AddExpression expression, StorageLocation leftLocation) {
        Expression left = expression.getLeft();
        Expression right = expression.getRight();

        Type leftType = types().getType(left);
        Type rightType = types().getType(right);

        // If this is a string addition (concatenation)
        if (leftType instanceof Str && rightType instanceof Str) {
            CodeContainer cc = new CodeContainer();

            cc.add(Blank.INSTANCE);
            cc.add(new AssemblyComment("--- " + expression + " -->"));

            // Generate code for left sub expression, and store result in leftLocation
            cc.addAll(codeGenerator.expression(expression.getLeft(), leftLocation));

            try (StorageLocation rightLocation = storageFactory().allocateNonVolatile(rightType);
                 StorageLocation tmpLocation = storageFactory().allocateNonVolatile(I64.INSTANCE)) {
                final var rcx = storageFactory().get(RCX);
                final var rdx = storageFactory().get(RDX);

                // Generate code for right sub expression, and store result in rightLocation
                cc.addAll(codeGenerator.expression(expression.getRight(), rightLocation));

                // Calculate length of result string
                cc.add(new AssemblyComment("Calculate length of strings to add (" + leftLocation + " and " + rightLocation + ")"));

                rcx.moveLocToThis(leftLocation, cc);
                cc.addAll(Snippets.strlen(RCX));
                cc.add(new AssemblyComment("Move length (rax) to tmp location (" + tmpLocation + ")"));
                tmpLocation.moveLocToThis(storageFactory().get(RAX), cc);

                rcx.moveLocToThis(rightLocation, cc);
                cc.addAll(Snippets.strlen(RCX));
                cc.add(new AssemblyComment("Add length (rax) to tmp location (" + tmpLocation + ")"));
                tmpLocation.addLocToThis(storageFactory().get(RAX), cc);

                // Add one for the null character
                tmpLocation.incrementThis(cc);

                // Allocate memory for result string
                rcx.moveLocToThis(tmpLocation, cc);
                cc.addAll(Snippets.malloc(RCX));              // Address to new string in RAX

                // Copy left string to result string
                cc.add(new AssemblyComment("Copy left string (" + leftLocation + ") to result string (rax)"));
                rcx.moveRegToThis(RAX, cc);
                rdx.moveLocToThis(leftLocation, cc);
                cc.addAll(Snippets.strcpy(RCX, RDX));         // Address to new string still in RAX

                // Copy right string to result string
                cc.add(new AssemblyComment("Copy right string (" + rightLocation + ") to result string (rax)"));
                rcx.moveRegToThis(RAX, cc);
                rdx.moveLocToThis(rightLocation, cc);
                cc.addAll(Snippets.strcat(RCX, RDX));         // Address to new string still in RAX

                // Save result value (address to new string) in tmpLocation
                cc.add(new AssemblyComment("Move result string (rax) to tmp location (" + tmpLocation + ")"));
                tmpLocation.moveRegToThis(RAX, cc);

                // Free any dynamic memory that we don't need any more
                if (allocatesDynamicMemory(left, leftType)) {
                    cc.add(new AssemblyComment("Free dynamic memory in " + leftLocation));
                    rcx.moveLocToThis(leftLocation, cc);
                    cc.addAll(Snippets.free(RCX));
                }
                if (allocatesDynamicMemory(right, rightType)) {
                    cc.add(new AssemblyComment("Free dynamic memory in " + rightLocation));
                    rcx.moveLocToThis(rightLocation, cc);
                    cc.addAll(Snippets.free(RCX));
                }

                // Move result to leftLocation where it is expected to be
                cc.add(new AssemblyComment("Move result string to expected storage location (" + leftLocation + ")"));
                leftLocation.moveLocToThis(tmpLocation, cc);
                cc.add(new AssemblyComment("<-- " + expression + " ---"));
                cc.add(Blank.INSTANCE);

                codeGenerator.addAllFunctionDependencies(Map.of(LIB_LIBC, Set.of(FUN_FREE, FUN_MALLOC, FUN_STRCAT, FUN_STRCPY, FUN_STRLEN)));
            }
            return cc.lines();
        } else {
            // Not a string addition, call super
            return super.generate(expression, leftLocation);
        }
    }
}
