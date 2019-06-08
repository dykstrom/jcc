/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.instruction.PopReg;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.functions.MemoryManagementUtils;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.Register.RCX;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_FREE;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * A function call helper that extends the default one with functionality for garbage collection
 * and memory management.
 *
 * @author Johan Dykstrom
 */
class GarbageCollectingFunctionCallHelper extends DefaultFunctionCallHelper {

    GarbageCollectingFunctionCallHelper(CodeGenerator codeGenerator, CodeContainer codeContainer, StorageFactory storageFactory, TypeManager typeManager) {
        super(codeGenerator, codeContainer, storageFactory, typeManager);
    }

    /**
     * Extends the default cleanup by also freeing any memory that was allocated
     * when evaluating the function arguments.
     */
    @Override
    void cleanUpRegisterArguments(List<Expression> args, List<StorageLocation> locations) {
        super.cleanUpRegisterArguments(args, locations);

        for (int i = 0; i < locations.size(); i++) {
            if (allocatesDynamicMemory(args.get(i))) {
                freeDynamicMemory(locations.get(i));
            }
        }
    }

    /**
     * Extends the default cleanup by also freeing any memory that was allocated
     * when evaluating the function arguments.
     */
    @Override
    void cleanUpStackArguments(List<Expression> args, int numberOfPushedArgs) {
        if (numberOfPushedArgs > 0) {
            List<Expression> expressions = args.subList(4, args.size());
            // If there is no expression that allocates dynamic memory, clean up the default way
            if (expressions.stream().noneMatch(this::allocatesDynamicMemory)) {
                super.cleanUpStackArguments(args, numberOfPushedArgs);
            } else {
                for (Expression expression : expressions) {
                    // Pop argument into RCX
                    add(new Comment("Popping " + expression));
                    add(new PopReg(RCX));
                    // If this expression allocated dynamic memory, free it
                    if (allocatesDynamicMemory(expression)) {
                        freeDynamicMemory(storageFactory.rcx);
                    }
                }
            }
        }
    }

    /**
     * Frees memory stored in the given storage location.
     */
    private void freeDynamicMemory(StorageLocation location) {
        add(new Comment("Free dynamic memory in " + location));
        storageFactory.rcx.moveLocToThis(location, codeContainer);
        addAll(Snippets.free(RCX));

        codeGenerator.addAllFunctionDependencies(MapUtils.of(LIB_LIBC, SetUtils.of(FUN_FREE)));
    }

    /**
     * Returns {@code true} if evaluating the given expression will allocate dynamic memory.
     */
    private boolean allocatesDynamicMemory(Expression expression) {
        return MemoryManagementUtils.allocatesDynamicMemory(expression, typeManager.getType(expression));
    }
}
