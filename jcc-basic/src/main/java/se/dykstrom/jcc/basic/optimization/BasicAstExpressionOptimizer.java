/*
 * Copyright 2024 Johan Dykström
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.dykstrom.jcc.basic.optimization;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.SqrtExpression;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.DefaultAstExpressionOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_SQR;

/**
 * The BASIC AST expression optimizer performs BASIC specific optimizations on the AST.
 *
 * @author Johan Dykstrom
 */
public class BasicAstExpressionOptimizer extends DefaultAstExpressionOptimizer {

    public BasicAstExpressionOptimizer(final TypeManager typeManager) {
        super(typeManager);
    }

    @Override
    public Expression expression(final Expression expression, final SymbolTable symbols) {
        if (expression instanceof FunctionCallExpression fce && (fce.getIdentifier().equals(FUN_SQR.getIdentifier()))) {
            return sqrtExpression(fce);
        }
        return super.expression(expression, symbols);
    }

    /**
     * Optimizes a function call to the 'sqr' function to a SqrtExpression.
     */
    private Expression sqrtExpression(final FunctionCallExpression fce) {
        final var args = fce.getArgs();
        if (args.size() == 1) {
            return new SqrtExpression(fce.line(), fce.column(), args.get(0));
        } else {
            return fce;
        }
    }
}
