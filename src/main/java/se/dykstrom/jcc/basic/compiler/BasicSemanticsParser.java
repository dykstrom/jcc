/*
 * Copyright (C) 2016 Johan Dykstrom
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

import se.dykstrom.jcc.basic.ast.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.*;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.utils.ExpressionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static se.dykstrom.jcc.basic.compiler.BasicTypeHelper.updateTypes;
import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_FMOD;

/**
 * The semantics parser for the Basic language. This parser enforces the semantic rules of the
 * language, including the correct use of line numbers and the type system. It returns a copy
 * of the parsed program, where some types are better defined than in the source program.
 *
 * The following rules define how the type of an identifier is decided:
 *
 * - If the identifier ends with a type specifier, like "$" for strings, the type specifier decides the type.
 * - If the identifier has been declared in a DIM statement, like "DIM a AS STRING", this decides the type.
 * - If the identifier starts with a letter used in a DEFtype statement, like "DEFSTR a-c", this decides the type.
 * - If neither of the above applies, the default type is used, and that is Double.
 *
 * @author Johan Dykstrom
 */
public class BasicSemanticsParser extends AbstractSemanticsParser {

    /** A set of all line numbers used in the program. */
    private final Set<String> lineNumbers = new HashSet<>();

    private final SymbolTable symbols = new SymbolTable();
    private final BasicTypeManager types;

    public BasicSemanticsParser(BasicTypeManager typeManager) {
        this.types = typeManager;
    }

    /**
     * Returns a reference to the symbol table.
     */
    public SymbolTable getSymbols() {
        return symbols;
    }

    /**
     * Returns a reference to the type manager.
     */
    public BasicTypeManager typeManager() { return types; }

    public Program program(Program program) {
        program.getStatements().forEach(this::lineNumber);
        List<Statement> statements = program.getStatements().stream().map(this::statement).toList();
        return program.withStatements(statements);
    }

    /**
     * Save line number of statement to the set of line numbers, and check that there are no duplicates.
     */
    private void lineNumber(final Statement statement) {
        final Statement actualStatement;
        if (statement instanceof LabelledStatement labelledStatement) {
            final String line = labelledStatement.label();
            if (lineNumbers.contains(line)) {
                final String msg = "duplicate line number: " + line;
                reportSemanticsError(statement.line(), statement.column(), msg, new DuplicateException(msg, labelledStatement.label()));
            } else {
                lineNumbers.add(line);
            }
            actualStatement = labelledStatement.statement();
        } else {
            actualStatement = statement;
        }
        
        // If this is a compound statement, also save line numbers of sub statements
        if (actualStatement instanceof IfStatement ifStatement) {
            ifStatement.getThenStatements().forEach(this::lineNumber);
            ifStatement.getElseStatements().forEach(this::lineNumber);
        } else if (actualStatement instanceof WhileStatement whileStatement) {
            whileStatement.getStatements().forEach(this::lineNumber);
        }
    }

    private Statement statement(Statement statement) {
        if (statement instanceof AssignStatement) {
            return assignStatement((AssignStatement) statement);
        } else if (statement instanceof AbstractDefTypeStatement) {
            return deftypeStatement((AbstractDefTypeStatement) statement);
        } else if (statement instanceof GosubStatement) {
            return jumpStatement((GosubStatement) statement);
        } else if (statement instanceof GotoStatement) {
            return jumpStatement((GotoStatement) statement);
        } else if (statement instanceof IfStatement) {
            return ifStatement((IfStatement) statement);
        } else if (statement instanceof LabelledStatement) {
            return labelledStatement((LabelledStatement) statement);
        } else if (statement instanceof LineInputStatement) {
            return lineInputStatement((LineInputStatement) statement);
        } else if (statement instanceof OnGosubStatement) {
            return onJumpStatement((OnGosubStatement) statement, "on-gosub");
        } else if (statement instanceof OnGotoStatement) {
            return onJumpStatement((OnGotoStatement) statement, "on-goto");
        } else if (statement instanceof PrintStatement) {
            return printStatement((PrintStatement) statement);
        } else if (statement instanceof SwapStatement) {
            return swapStatement((SwapStatement) statement);
        } else if (statement instanceof RandomizeStatement) {
            return randomizeStatement((RandomizeStatement) statement);
        } else if (statement instanceof VariableDeclarationStatement) {
            return variableDeclarationStatement((VariableDeclarationStatement) statement);
        } else if (statement instanceof WhileStatement) {
            return whileStatement((WhileStatement) statement);
        } else {
            return statement;
        }
    }

    private AssignStatement assignStatement(AssignStatement statement) {
        // Check and update expressions
        Expression lhsExpression = expression(statement.getLhsExpression());
        Expression rhsExpression = expression(statement.getRhsExpression());

        Type lhsType = getType(lhsExpression);
        Type rhsType = getType(rhsExpression);

        // Check that types are compatible
        if (!types.isAssignableFrom(lhsType, rhsType)) {
            String msg = "you cannot assign a value of type " + types.getTypeName(rhsType)
                    + " to a variable of type " + types.getTypeName(lhsType);
            reportSemanticsError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, rhsType));
        }

        // Return updated statement with the possibly updated expressions
        return statement.withLhsExpression((IdentifierExpression) lhsExpression).withRhsExpression(rhsExpression);
    }

    private VariableDeclarationStatement variableDeclarationStatement(VariableDeclarationStatement statement) {
        // For each declaration
        statement.getDeclarations().forEach(declaration -> {
            // Check identifier
            String name = declaration.getName();
            Type type = declaration.getType();

            // If the variable name has a type specifier, it must match the type
            final var optionalSpecifiedType = types.getTypeByTypeSpecifier(name);
            optionalSpecifiedType.ifPresent(specifiedType -> {
                if (hasInvalidTypeSpecifier(type, specifiedType)) {
                    String msg = "variable '" + name + "' is defined with type specifier "
                            + types.getTypeName(specifiedType) + " and type " + types.getTypeName(type);
                    reportSemanticsError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
                }
            });

            if (type instanceof Arr) {
                ArrayDeclaration arrayDeclaration = (ArrayDeclaration) declaration;
                List<Expression> subscripts = arrayDeclaration.getSubscripts().stream().map(this::expression).toList();

                // Check that (array) identifier is not defined in symbol table
                if (symbols.containsArray(name)) {
                    String msg = "variable '" + name + "' is already defined, with type " + types.getTypeName(symbols.getArrayType(name));
                    reportSemanticsError(statement.line(), statement.column(), msg, new DuplicateException(msg, name));
                }
                // Check that array subscripts are of type integer
                if (!allSubscriptsAreIntegers(subscripts)) {
                    String msg = "array '" + name + "' has non-integer subscript";
                    reportSemanticsError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
                }
                // $DYNAMIC arrays are not implemented yet
                if (isDynamicArray(subscripts)) {
                    String msg = "$DYNAMIC arrays not supported yet";
                    reportSemanticsError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
                }

                // Possibly adjust subscript expressions if OPTION BASE is 0
                arrayDeclaration.setSubscripts(adjustSubscriptsForOptionBase(subscripts));

                // Add variable to symbol table
                symbols.addArray(new Identifier(name, type), arrayDeclaration);
            } else {
                // Check that identifier is not defined in symbol table
                if (symbols.contains(name)) {
                    String msg = "variable '" + name + "' is already defined, with type " + types.getTypeName(symbols.getType(name));
                    reportSemanticsError(statement.line(), statement.column(), msg, new DuplicateException(msg, name));
                }
                // Add variable to symbol table
                symbols.addVariable(new Identifier(name, type));
            }
        });

        return statement;
    }

    /**
     * Returns a list of subscript expressions that have been adjusted to comply with the current OPTION BASE.
     * If OPTION BASE is 0 (default), all subscript expressions are increased by 1 to account for the extra array
     * element at index 0. If OPTION BASE is 1 (not supported yet), the subscript expressions are not modified.
     *
     * The upper bound in an array declaration in Basic is included in the range of indices allowed, so the
     * declaration "DIM A(5)" declares an array with elements A(0) to A(5) if OPTION BASE is 0.
     */
    private List<Expression> adjustSubscriptsForOptionBase(List<Expression> subscripts) {
        return subscripts.stream()
                .map(e -> new AddExpression(e.line(), e.column(), e, IntegerLiteral.ONE))
                .collect(toList());
    }

    /**
     * Returns {@code true} if all array subscripts are integers.
     */
    private boolean allSubscriptsAreIntegers(List<Expression> subscripts) {
        return ExpressionUtils.areAllIntegerExpressions(subscripts, types);
    }

    /**
     * Returns {@code true} if the array subscripts signal a $DYNAMIC array, that is,
     * the subscripts are not defined by constant expressions only.
     */
    private boolean isDynamicArray(List<Expression> subscripts) {
        return !ExpressionUtils.areAllConstantExpressions(subscripts);
    }

    /**
     * Returns {@code true} if {@code specifiedType} does not match {@code declaredType}.
     *
     * @see BasicSyntaxVisitor#visitIdent(BasicParser.IdentContext)
     */
    private boolean hasInvalidTypeSpecifier(final Type declaredType, final Type specifiedType) {
        if (declaredType instanceof Arr array) {
            return !specifiedType.equals(array.getElementType());
        }
        return !specifiedType.equals(declaredType);
    }

    /**
     * Parses a DEFtype statement. We don't need to define the type in the type manager
     * because we already did in BasicSyntaxVisitor.
     */
    private Statement deftypeStatement(AbstractDefTypeStatement statement) {
        if (statement.getLetters().isEmpty()) {
            String msg = "invalid letter interval in " + statement.getKeyword().toLowerCase();
            reportSemanticsError(statement.line(), statement.column(), msg, new InvalidException(msg, null));
        }
        return statement;
    }

    private AbstractJumpStatement jumpStatement(AbstractJumpStatement statement) {
        String line = statement.getJumpLabel();
        if (!lineNumbers.contains(line)) {
            String msg = "undefined line number: " + line;
            reportSemanticsError(statement.line(), statement.column(), msg, new UndefinedException(msg, line));
        }
        return statement;
    }

    private IfStatement ifStatement(IfStatement statement) {
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE) && !type.equals(Bool.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in if statement";
            reportSemanticsError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
        }

        // Process all sub statements recursively
        List<Statement> thenStatements = statement.getThenStatements().stream().map(this::statement).toList();
        List<Statement> elseStatements = statement.getElseStatements().stream().map(this::statement).toList();
        
        return statement.withExpression(expression).withThenStatements(thenStatements).withElseStatements(elseStatements);
    }

    private Statement labelledStatement(LabelledStatement labelledStatement) {
        return labelledStatement.withStatement(statement(labelledStatement.statement()));
    }

    private LineInputStatement lineInputStatement(LineInputStatement statement) {
        statement = updateTypes(statement, symbols);

        Identifier identifier = statement.identifier();
        Type type = identifier.type();
        if (!type.equals(Str.INSTANCE)) {
            String msg = "expected identifier of type " + types.getTypeName(Str.INSTANCE) + ", not " + types.getTypeName(type);
            reportSemanticsError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
        }

        // Save the identifier for later
        symbols.addVariable(identifier);

        return statement;
    }

    private AbstractOnJumpStatement onJumpStatement(AbstractOnJumpStatement statement, String statementName) {
        // Check expression
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in " + statementName + " statement";
            reportSemanticsError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
        }

        // Check jump labels
        statement.getJumpLabels().stream()
            .filter(label -> !lineNumbers.contains(label))
            .forEach(label -> {
                String msg = "undefined line number/label: " + label;
                reportSemanticsError(statement.line(), statement.column(), msg, new UndefinedException(msg, label));
            });
        return statement;
    }

    private PrintStatement printStatement(PrintStatement statement) {
        List<Expression> expressions = statement.getExpressions().stream().map(this::expression).toList();
        return statement.withExpressions(expressions);
    }

    private RandomizeStatement randomizeStatement(RandomizeStatement statement) {
        Expression expression = statement.getExpression();
        if (expression != null) {
            return statement.withExpression(expression(expression));
        } else {
            return statement;
        }
    }

    private SwapStatement swapStatement(SwapStatement statement) {
        IdentifierExpression first = (IdentifierExpression) expression(statement.first());
        IdentifierExpression second = (IdentifierExpression) expression(statement.second());

        Type firstType = first.getType();
        Type secondType = second.getType();

        boolean swappable = types.isAssignableFrom(firstType, secondType) && types.isAssignableFrom(secondType, firstType);
        if (!swappable) {
            String msg = "cannot swap variables with types " + firstType + " and " + secondType;
            reportSemanticsError(statement.line(), statement.column(), msg, new SemanticsException(msg));
        }
        return statement;
    }

    private WhileStatement whileStatement(WhileStatement statement) {
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE) && !type.equals(Bool.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in while statement";
            reportSemanticsError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
        }

        // Process all sub statements recursively
        List<Statement> statements = statement.getStatements().stream().map(this::statement).toList();
        
        return statement.withExpression(expression).withStatements(statements);
    }

    private Expression expression(Expression expression) {
        if (expression instanceof BinaryExpression binaryExpression) {
            Expression left = expression(binaryExpression.getLeft());
            Expression right = expression(binaryExpression.getRight());
            checkDivisionByZero(expression);

            // If this is a MOD expression involving floats, call library function fmod
            // We cannot check the type of the entire expression, because it has not yet been updated with correct types
            if (expression instanceof ModExpression && (getType(left) instanceof F64 || getType(right) instanceof F64)) {
                expression = functionCall(new FunctionCallExpression(expression.line(), expression.column(), FUN_FMOD.getIdentifier(), asList(left, right)));
            } else {
                expression = binaryExpression.withLeft(left).withRight(right);
                checkType((BinaryExpression) expression);
            }
        } else if (expression instanceof FunctionCallExpression) {
            expression = functionCall((FunctionCallExpression) expression);
        } else if (expression instanceof ArrayAccessExpression) {
            expression = arrayAccessExpression((ArrayAccessExpression) expression);
        } else if (expression instanceof IdentifierDerefExpression) {
            expression = identifierDerefExpression((IdentifierDerefExpression) expression);
        } else if (expression instanceof IdentifierNameExpression) {
            expression = identifierNameExpression((IdentifierNameExpression) expression);
        } else if (expression instanceof IntegerLiteral) {
            checkInteger((IntegerLiteral) expression);
        } else if (expression instanceof UnaryExpression unaryExpression) {
            Expression subExpr = expression(unaryExpression.getExpression());
            expression = unaryExpression.withExpression(subExpr);
            checkType((UnaryExpression) expression);
        }
        return expression;
    }

    /**
     * Parses a function call expression. A FCE may also turn out be an array access expression,
     * in which case this method will instead return an array access expression.
     */
	private Expression functionCall(FunctionCallExpression fce) {
        // Check and update arguments
        List<Expression> args = fce.getArgs().stream().map(this::expression).toList();
        // Get types of arguments
        List<Type> argTypes = types.getTypes(args);

        Identifier identifier = fce.getIdentifier();
        String name = identifier.name();

        if (symbols.containsArray(name) && functionCallArgsAreActuallyArrayIndices(argTypes, name)) {
            // If the identifier is actually an array identifier
            Type arrayType = symbols.getArrayType(name);
            return new ArrayAccessExpression(fce.line(), fce.column(), identifier.withType(arrayType), args);
        } else if (symbols.containsFunction(name)) {
            // If the identifier is a function identifier
            try {
                // Match the function with the expected argument types
                Function function = types.resolveFunction(name, argTypes, symbols);
                identifier = function.getIdentifier();
            } catch (SemanticsException e) {
                reportSemanticsError(fce.line(), fce.column(), e.getMessage(), e);
                // Make sure the type is a function, so we can continue parsing
                identifier = identifier.withType(Fun.from(argTypes, F64.INSTANCE));
            }
        } else {
            String msg = "undefined function: " + name;
            reportSemanticsError(fce.line(), fce.column(), msg, new UndefinedException(msg, name));
        }

	    return fce.withIdentifier(identifier).withArgs(args);
    }

    /**
     * Returns {@code true} if the list of function call argument types are actually indices in an array access.
     * The arguments must all be of integer type, and must be as many as the number of array dimensions.
     */
    private boolean functionCallArgsAreActuallyArrayIndices(List<Type> argTypes, String name) {
        if (argTypes.isEmpty()) {
            return false;
        }
        if (!argTypes.stream().allMatch(type -> type instanceof I64)) {
            return false;
        }
        return argTypes.size() == symbols.getArrayType(name).getDimensions();
    }

    private Expression arrayAccessExpression(ArrayAccessExpression expression) {
        final List<Expression> subscripts = expression.getSubscripts().stream().map(this::expression).toList();
        Identifier identifier = expression.getIdentifier();
        final String name = identifier.name();
        final Type type = identifier.type();
        if (!allSubscriptsAreIntegers(subscripts)) {
            final String msg = "subscripts must be integers, array '" + name + "'";
            reportSemanticsError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
        }
        if (symbols.containsArray(name)) {
            // If the identifier is present in the symbol table, reuse that one
            identifier = symbols.getArrayIdentifier(name);
        }
        return expression.withIdentifier(identifier).withSubscripts(subscripts);
    }

    private Expression identifierNameExpression(IdentifierNameExpression expression) {
        String name = expression.getIdentifier().name();
        if (symbols.contains(name)) {
            return expression.withIdentifier(symbols.getIdentifier(name));
        } else {
            symbols.addVariable(expression.getIdentifier());
            return expression;
        }
    }

    /**
     * Parses an identifier dereference expression. An IDE may also turn out be a function call
     * to a function with no arguments, in which case this method will instead return a function
     * call expression.
     */
    private Expression identifierDerefExpression(IdentifierDerefExpression ide) {
        String name = ide.getIdentifier().name();
        if (symbols.contains(name)) {
            // If the identifier is present in the symbol table, reuse that one
            Identifier definedIdentifier = symbols.getIdentifier(name);
            return ide.withIdentifier(definedIdentifier);
        } else if (symbols.containsFunction(name)) {
            // Identifier is a function with no arguments, return a function call expression instead
            Identifier definedIdentifier = symbols.getFunctionIdentifier(name, emptyList());
            return new FunctionCallExpression(ide.line(), ide.column(), definedIdentifier, emptyList());
        } else {
            // If the identifier is undefined, add it to the symbol table now
            symbols.addVariable(ide.getIdentifier());
            return ide;
        }
    }

    private void checkInteger(IntegerLiteral integer) {
        String value = integer.getValue();
        try {
            Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            String msg = "integer out of range: " + value;
            reportSemanticsError(integer.line(), integer.column(), msg, new InvalidException(msg, value));
        }
    }

    private void checkDivisionByZero(Expression expression) {
		if (expression instanceof DivExpression || expression instanceof IDivExpression || expression instanceof ModExpression) {
			Expression right = ((BinaryExpression) expression).getRight();
			if (right instanceof LiteralExpression) {
				String value = ((LiteralExpression) right).getValue();
				if (isZero(value)) {
		            String msg = "division by zero: " + value;
		            reportSemanticsError(expression.line(), expression.column(), msg, new InvalidException(msg, value));
				}
			}
		}
	}

    /**
     * Returns {@code true} if the string {@code value} represents a zero value.
     */
    private boolean isZero(String value) {
        Pattern zeroPattern = Pattern.compile("0(\\.0*)?");
        return zeroPattern.matcher(value).matches();
    }

    private void checkType(UnaryExpression expression) {
        Type type = getType(expression.getExpression());
        
        if (expression instanceof ConditionalExpression) {
            // Conditional expressions require subexpression to be boolean
            if (!type.equals(Bool.INSTANCE)) {
                String msg = "expected subexpression of type boolean: " + expression;
                reportSemanticsError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
            }
        } else {
            getType(expression);
        }
    }

    private void checkType(BinaryExpression expression) {
        Type leftType = getType(expression.getLeft());
        Type rightType = getType(expression.getRight());

        if (expression instanceof ConditionalExpression) {
            // Conditional expressions require both subexpressions to be boolean
            if (leftType instanceof Bool && rightType instanceof Bool) {
                return;
            } else {
                String msg = "expected subexpressions of type boolean: " + expression;
                reportSemanticsError(expression.line(), expression.column(), msg, new SemanticsException(msg));
            }
        } else if (expression instanceof RelationalExpression) {
            // Relational expressions require both subexpressions to be either strings or numbers
            if (leftType instanceof NumericType && rightType instanceof NumericType) {
                return;
            } else if (leftType instanceof Str && rightType instanceof Str) {
                return;
            } else {
                String msg = "cannot compare " + types.getTypeName(leftType) + " and " + types.getTypeName(rightType);
                reportSemanticsError(expression.line(), expression.column(), msg, new SemanticsException(msg));
            }
        } else if (expression instanceof IDivExpression) {
            if (leftType instanceof I64 && rightType instanceof I64) {
                return;
            } else {
                String msg = "expected subexpressions of type integer: " + expression;
                reportSemanticsError(expression.line(), expression.column(), msg, new SemanticsException(msg));
            }
        } else {
            getType(expression);
        }
    }

    private Type getType(Expression expression) {
        try {
            return types.getType(expression);
        } catch (SemanticsException se) {
            reportSemanticsError(expression.line(), expression.column(), se.getMessage(), se);
            return F64.INSTANCE;
        }
    }
}
