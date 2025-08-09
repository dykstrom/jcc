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

import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.instruction.PopReg;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.functions.MemoryManagementUtils;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static se.dykstrom.jcc.common.assembly.base.Register.RCX;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_FREE_I64;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * A function call helper that extends the default one with functionality for garbage collection
 * and memory management.
 *
 * @author Johan Dykstrom
 */
class GarbageCollectingFunctionCallHelper extends DefaultFunctionCallHelper {

    GarbageCollectingFunctionCallHelper(final AsmCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    /**
     * Extends the default cleanup by also freeing any memory that was allocated
     * when evaluating the function arguments.
     */
    @Override
    void cleanUpRegisterArguments(List<Expression> args, List<StorageLocation> locations, CodeContainer cc) {
        super.cleanUpRegisterArguments(args, locations, cc);

        for (int i = 0; i < locations.size(); i++) {
            if (allocatesDynamicMemory(args.get(i))) {
                freeDynamicMemory(locations.get(i), cc);
            }
        }
    }

    /**
     * Extends the default cleanup by also freeing any memory that was allocated
     * when evaluating the function arguments.
     */
    @Override
    void cleanUpStackArguments(List<Expression> args, int numberOfPushedArgs, CodeContainer cc) {
        if (numberOfPushedArgs > 0) {
            List<Expression> expressions = args.subList(4, args.size());
            // If there is no expression that allocates dynamic memory, clean up the default way
            if (expressions.stream().noneMatch(this::allocatesDynamicMemory)) {
                super.cleanUpStackArguments(args, numberOfPushedArgs, cc);
            } else {
                for (Expression expression : expressions) {
                    // Pop argument into RCX
                    cc.add(new AssemblyComment("Popping " + expression));
                    cc.add(new PopReg(RCX));
                    // If this expression allocated dynamic memory, free it
                    if (allocatesDynamicMemory(expression)) {
                        freeDynamicMemory(codeGenerator.storageFactory().get(RCX), cc);
                    }
                }
            }
        }
    }

    /**
     * Frees memory stored in the given storage location.
     */
    private void freeDynamicMemory(StorageLocation location, CodeContainer cc) {
        cc.add(new AssemblyComment("Free dynamic memory in " + location));
        codeGenerator.storageFactory().get(RCX).moveLocToThis(location, cc);
        cc.addAll(Snippets.free(RCX));

        codeGenerator.addAllFunctionDependencies(Map.of(LIB_LIBC, Set.of(CF_FREE_I64)));
    }

    /**
     * Returns {@code true} if evaluating the given expression will allocate dynamic memory.
     */
    private boolean allocatesDynamicMemory(Expression expression) {
        return MemoryManagementUtils.allocatesDynamicMemory(expression, codeGenerator.typeManager().getType(expression));
    }
}
