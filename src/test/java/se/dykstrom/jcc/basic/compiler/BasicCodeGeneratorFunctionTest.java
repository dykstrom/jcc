/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.basic.compiler;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.dykstrom.jcc.basic.compiler.BasicLibraryFunctions.IDENT_FUN_ABS;
import static se.dykstrom.jcc.basic.compiler.BasicLibraryFunctions.IDENT_FUN_LEN;
import static se.dykstrom.jcc.common.compiler.CompilerUtils.LIB_LIBC;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect;
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.MoveMemToReg;
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToMem;
import se.dykstrom.jcc.common.assembly.other.DataDefinition;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

public class BasicCodeGeneratorFunctionTest extends AbstractBasicCodeGeneratorTest {

    public static final Identifier IDENT_FUN_FOO = new Identifier("foo", Fun.from(I64.INSTANCE));

    public static final LibraryFunction FUN_FOO = new LibraryFunction(
            IDENT_FUN_FOO.getName(), 
            ((Fun) IDENT_FUN_FOO.getType()).getReturnType(), 
            asList(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), 
            MapUtils.of(LIB_LIBC, SetUtils.of("fooo")), 
            "fooo"
    );

    @Before
    public void setUp() {
        // Define some functions for testing
        defineFunction(IDENT_FUN_ABS, BasicLibraryFunctions.FUN_ABS);
        defineFunction(IDENT_FUN_LEN, BasicLibraryFunctions.FUN_LEN);
        defineFunction(IDENT_FUN_FOO, FUN_FOO);
    }

    @Test
    public void shouldGenerateSingleFunctionCall() {
        Expression fe = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(IL_1));
        Statement ps = new PrintStatement(0, 0, singletonList(fe));
        
        AsmProgram result = assembleProgram(singletonList(ps));
        List<Code> codes = result.codes();
        
        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg.class, codes));
        // Three calls: abs, printf, and exit
        assertCodes(codes, 1, 3, 1, 3);
        assertTrue(codes.stream()
                .filter(code -> code instanceof CallIndirect)
                .map(code -> (CallIndirect) code)
                .anyMatch(move -> move.getTarget().contains(IDENT_FUN_ABS.getName())));
    }

    @Test
    public void shouldGenerateFunctionCallWithStringArg() {
        Expression fe = new FunctionCallExpression(0, 0, IDENT_FUN_LEN, singletonList(SL_ONE));
        Statement ps = new PrintStatement(0, 0, singletonList(fe));
        
        AsmProgram result = assembleProgram(singletonList(ps));
        List<Code> codes = result.codes();
        
        // Three moves: format string, string expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg.class, codes));
        // Three calls: abs, printf, and exit
        assertCodes(codes, 1, 3, 1, 3);
        assertTrue(codes.stream()
                .filter(code -> code instanceof CallIndirect)
                .map(code -> (CallIndirect) code)
                .anyMatch(move -> move.getTarget().contains(IDENT_FUN_LEN.getName())));
    }

    @Test
    public void shouldGenerateNestedFunctionCall() {
        Expression fe1 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(IL_1));
        Expression fe2 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe1));
        Expression fe3 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe2));
        Statement ps = new PrintStatement(0, 0, singletonList(fe3));
        
        AsmProgram result = assembleProgram(singletonList(ps));
        List<Code> codes = result.codes();
        
        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg.class, codes));
        // Five calls: abs*3, printf, and exit
        assertCodes(codes, 1, 3, 1, 5);
    }

    @Test
    public void shouldGenerateDeeplyNestedFunctionCall() {
        Expression fe1 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(IL_1));
        Expression fe2 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe1));
        Expression fe3 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe2));
        Expression fe4 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe3));
        Expression fe5 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe4));
        Expression fe6 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe5));
        Statement ps = new PrintStatement(0, 0, singletonList(fe6));
        
        AsmProgram result = assembleProgram(singletonList(ps));
        //System.out.println(result.toAsm());
        List<Code> codes = result.codes();
        
        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg.class, codes));
        // Eight calls: abs*6, printf, and exit
        assertCodes(codes, 1, 3, 1, 8);
    }

    /**
     * Tests that we can encode a deeply nested function call to a function with many arguments,
     * even though we run out of registers to store evaluated arguments in. In that case, temporary
     * variables (memory addresses) will be used instead.
     */
    @Test
    public void shouldGenerateNestedFunctionCallWithManyArgs() {
        Expression fe1 = new FunctionCallExpression(0, 0, IDENT_FUN_FOO, asList(IL_1, IL_2, IL_1));
        Expression fe2 = new FunctionCallExpression(0, 0, IDENT_FUN_FOO, asList(IL_3, IL_4, IL_3));
        Expression fe3 = new FunctionCallExpression(0, 0, IDENT_FUN_FOO, asList(fe1, fe2, IL_2));
        Expression fe4 = new FunctionCallExpression(0, 0, IDENT_FUN_FOO, asList(fe1, fe2, IL_4));
        Expression fe5 = new FunctionCallExpression(0, 0, IDENT_FUN_FOO, asList(fe3, fe4, IL_1));
        Expression fe6 = new FunctionCallExpression(0, 0, IDENT_FUN_FOO, asList(fe5, IL_4, IL_3));
        Statement ps = new PrintStatement(0, 0, asList(fe6, fe6, fe6));
        
        AsmProgram result = assembleProgram(singletonList(ps));
        List<Code> codes = result.codes();
        
        // We should be able to find at least one case where an evaluated argument is moved to and from a temporary variable
        assertTrue(codes.stream()
                .filter(code -> code instanceof DataDefinition)
                .map(code -> (DataDefinition) code)
                .anyMatch(data -> data.getIdentifier().getMappedName().startsWith("__tmp")));
        assertTrue(codes.stream()
                .filter(code -> code instanceof MoveRegToMem)
                .map(code -> (MoveRegToMem) code)
                .anyMatch(move -> move.getMemory().startsWith("__tmp"))); // Mapped name
        assertTrue(codes.stream()
                .filter(code -> code instanceof MoveMemToReg)
                .map(code -> (MoveMemToReg) code)
                .anyMatch(move -> move.getMemory().startsWith("__tmp"))); // Mapped name
    }
}
