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

package se.dykstrom.jcc.basic.code.statement;

import se.dykstrom.jcc.basic.ast.SwapStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.assembly.base.*;
import se.dykstrom.jcc.common.ast.IdentifierExpression;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGeneratorComponent;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.storage.RegisterStorageLocation;
import se.dykstrom.jcc.common.types.*;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.CodeContainer.withCodeContainer;

public class SwapCodeGenerator extends AbstractStatementCodeGeneratorComponent<SwapStatement, BasicTypeManager, BasicCodeGenerator> {

    private final RegisterStorageLocation rcx = storageFactory.rcx;
    private final RegisterStorageLocation rdx = storageFactory.rdx;

    public SwapCodeGenerator(Context context) {
        super(context);
    }

    @Override
    public List<Line> generate(SwapStatement statement) {
        var cc = new CodeContainer();

        getLabel(statement).ifPresent(cc::add);
        cc.add(getComment(statement));

        var first = statement.getFirst();
        var second = statement.getSecond();

        var firstType = types.getType(first);
        var secondType = types.getType(second);

        cc.addAll(codeGenerator.withAddressOfIdentifier(first, (firstBase, firstOffset) ->
                codeGenerator.withAddressOfIdentifier(second, (secondBase, secondOffset) -> withCodeContainer(it -> {
                    var firstAddress = firstBase + firstOffset;
                    var secondAddress = secondBase + secondOffset;
                    if (firstType.equals(secondType)) {
                        // If types are equal, we can just swap their values
                        swapEqualTypes(first, second, firstAddress, secondAddress, it);
                    } else {
                        // Otherwise, we need to convert values while swapping them
                        swapUnequalTypes(first, second, firstAddress, secondAddress, firstType, secondType, it);
                    }
                    if (firstType instanceof Str) {
                        // If the variables are strings, we also need to swap the variable type pointers used for GC
                        swapTypePointers(firstBase, firstOffset, secondBase, secondOffset, it);
                    }
                }))
        ));

        return cc.lines();
    }

    private void swapEqualTypes(IdentifierExpression first, IdentifierExpression second,
                                String firstAddress, String secondAddress,
                                CodeContainer codeContainer) {
        codeContainer.add(new Comment("Swapping " + first + " and " + second));
        rcx.moveMemToThis(firstAddress, codeContainer);
        rdx.moveMemToThis(secondAddress, codeContainer);
        rcx.moveThisToMem(secondAddress, codeContainer);
        rdx.moveThisToMem(firstAddress, codeContainer);
    }

    private void swapUnequalTypes(IdentifierExpression first, IdentifierExpression second,
                                  String firstAddress, String secondAddress,
                                  Type firstType, Type secondType,
                                  CodeContainer codeContainer) {
        codeContainer.add(new Comment("Swapping and converting " + first + " and " + second));
        try (var firstLocation = storageFactory.allocateNonVolatile(firstType);
             var secondLocation = storageFactory.allocateNonVolatile(secondType);
             var tmpLocation = storageFactory.allocateNonVolatile(secondType)) {
            // Read values from memory
            firstLocation.moveMemToThis(firstAddress, codeContainer);
            secondLocation.moveMemToThis(secondAddress, codeContainer);

            // Convert and write first value
            tmpLocation.convertAndMoveLocToThis(firstLocation, codeContainer);
            tmpLocation.moveThisToMem(secondAddress, codeContainer);

            // Convert and write second value
            firstLocation.convertAndMoveLocToThis(secondLocation, codeContainer);
            firstLocation.moveThisToMem(firstAddress, codeContainer);
        }
    }

    private void swapTypePointers(String firstBase, String firstOffset, String secondBase, String secondOffset, CodeContainer codeContainer) {
        String firstTypePointer = codeGenerator.deriveMappedTypeName(firstBase) + firstOffset;
        String secondTypePointer = codeGenerator.deriveMappedTypeName(secondBase) + secondOffset;

        codeContainer.add(new Comment("Swapping variable type pointers " + firstTypePointer + " and " + secondTypePointer));
        rcx.moveMemToThis(firstTypePointer, codeContainer);
        rdx.moveMemToThis(secondTypePointer, codeContainer);
        rcx.moveThisToMem(secondTypePointer, codeContainer);
        rdx.moveThisToMem(firstTypePointer, codeContainer);
    }
}
