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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * - If the identifier is the LHS in an assignment, the type is derived from the RHS expression.
 * - If the identifier is used in a statement that requires a certain type, like "LINE INPUT a", this decides the type.
 * - If neither of the above applies, the default type is used, and that is integer.
 *
 * @author Johan Dykstrom
 */
class BasicSemanticsParser extends AbstractSemanticsParser {

    /** A set of all line numbers used in the program. */
    private final Set<String> lineNumbers = new HashSet<>();

    private final SymbolTable symbols = new SymbolTable();
    private final BasicTypeManager types = new BasicTypeManager();

    /**
     * Returns a reference to the symbol table.
     */
    public SymbolTable getSymbols() {
        return symbols;
    }

    public Program program(Program program) {
        program.getStatements().forEach(this::lineNumber);
        List<Statement> statements = program.getStatements().stream().map(this::statement).collect(toList());
        return program.withStatements(statements);
    }

    /**
     * Save line number of statement to the set of line numbers, and check that there are no duplicates.
     */
    private void lineNumber(Statement statement) {
        String line = statement.getLabel();
        if (line != null) {
            if (lineNumbers.contains(line)) {
                String msg = "duplicate line number: " + line;
                reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new DuplicateException(msg, statement.getLabel()));
            } else {
                lineNumbers.add(line);
            }
        }
        
        // If this is a compound statement, also save line numbers of sub statements
        if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            ifStatement.getThenStatements().forEach(this::lineNumber);
            ifStatement.getElseStatements().forEach(this::lineNumber);
        } else if (statement instanceof WhileStatement) {
            WhileStatement whileStatement = (WhileStatement) statement;
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
        // Check and update expression
        Expression expression = expression(statement.getExpression());

        // Check identifier
        String name = statement.getIdentifier().getName();

        // If the identifier was already defined, use the old definition
        Identifier identifier = symbols.contains(name) ? symbols.getIdentifier(name) : statement.getIdentifier();

        Type identType = identifier.getType();
        Type exprType = getType(expression);

        // If the identifier has no type, look it up using type manager
        if (identType instanceof Unknown) {
            identType = types.getIdentType(name);
        }
        // If the identifier still has no type, derive the type from the expression
        if (identType instanceof Unknown) {
            identType = exprType;
        }
        // Update identifier with possibly new type
        identifier = identifier.withType(identType);

        // Save the updated identifier for later
        symbols.addVariable(identifier);

        // Check that expression can be assigned to identifier
        if (!types.isAssignableFrom(identType, exprType)) {
            String msg = "you cannot assign a value of type " + types.getTypeName(exprType)
                    + " to variable '" + name + "' of type " + types.getTypeName(identType);
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new InvalidTypeException(msg, exprType));
        }

        // Return updated statement with the possibly updated identifier and expression
        return statement.withIdentifier(identifier).withExpression(expression);
    }

    private VariableDeclarationStatement variableDeclarationStatement(VariableDeclarationStatement statement) {
        // For each declaration
        statement.getDeclarations().forEach(declaration -> {
            // Check identifier
            String name = declaration.getName();
            Type type = declaration.getType();

            // Check that identifier is not defined in symbol table
            if (symbols.contains(name)) {
                String msg = "variable '" + name + "' is already defined, with type " + types.getTypeName(symbols.getType(name));
                reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new DuplicateException(msg, null));
            }

            // Check that name does not have a type specifier
            if (hasTypeSpecifier(name)) {
                String msg = "variable '" + name + "' is defined with type specifier";
                reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new DuplicateException(msg, null));
            }

            // Add variable to symbol table
            symbols.addVariable(new Identifier(name, type));
        });

        return statement;
    }

    /**
     * Returns {@code true} if the given variable name ends with a type specifier.
     *
     * @see BasicSyntaxVisitor#visitIdent(BasicParser.IdentContext)
     */
    private boolean hasTypeSpecifier(String name) {
        return name.endsWith("%") || name.endsWith("$") || name.endsWith("_hash");
    }

    private Statement deftypeStatement(AbstractDefTypeStatement statement) {
        if (statement.getLetters().isEmpty()) {
            String msg = "invalid letter interval in " + statement.getKeyword().toLowerCase();
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new InvalidException(msg, null));
        } else {
            types.defineIdentType(statement.getLetters(), statement.getType());
        }
        return statement;
    }

    private AbstractJumpStatement jumpStatement(AbstractJumpStatement statement) {
        String line = statement.getJumpLabel();
        if (!lineNumbers.contains(line)) {
            String msg = "undefined line number: " + line;
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new UndefinedException(msg, line));
        }
        return statement;
    }

    private IfStatement ifStatement(IfStatement statement) {
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE) && !type.equals(Bool.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in if statement";
            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new InvalidTypeException(msg, type));
        }

        // Process all sub statements recursively
        List<Statement> thenStatements = statement.getThenStatements().stream().map(this::statement).collect(toList());
        List<Statement> elseStatements = statement.getElseStatements().stream().map(this::statement).collect(toList());
        
        return statement.withExpression(expression).withThenStatements(thenStatements).withElseStatements(elseStatements);
    }

    private LineInputStatement lineInputStatement(LineInputStatement statement) {
        statement = updateTypes(statement, symbols, types);

        Identifier identifier = statement.identifier();
        Type type = identifier.getType();
        if (!type.equals(Str.INSTANCE)) {
            String msg = "expected identifier of type " + types.getTypeName(Str.INSTANCE) + ", not " + types.getTypeName(type);
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new InvalidTypeException(msg, type));
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
            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new InvalidTypeException(msg, type));
        }

        // Check jump labels
        statement.getJumpLabels().stream()
            .filter(label -> !lineNumbers.contains(label))
            .forEach(label -> {
                String msg = "undefined line number/label: " + label;
                reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new UndefinedException(msg, label));
            });
        return statement;
    }

    private PrintStatement printStatement(PrintStatement statement) {
        List<Expression> expressions = statement.getExpressions().stream().map(this::expression).collect(toList());
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
        statement = updateTypes(statement, symbols, types);

        Identifier first = statement.getFirst();
        Identifier second = statement.getSecond();

        Type firstType = first.getType();
        Type secondType = second.getType();

        if (firstType instanceof Unknown && secondType instanceof Unknown) {
            String msg = "cannot swap two variables of unknown type";
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new InvalidTypeException(msg, Unknown.INSTANCE));
        }

        // Save the updated identifiers for later
        symbols.addVariable(first);
        symbols.addVariable(second);

        // Variables can be swapped if they have the same type, or if both are numeric
        boolean swappable = firstType.equals(secondType) || (firstType instanceof NumericType && secondType instanceof NumericType);
        if (!swappable) {
            String msg = "cannot swap variables with types " + firstType + " and " + secondType;
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new SemanticsException(msg));
        }
        return statement;
    }

    private WhileStatement whileStatement(WhileStatement statement) {
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE) && !type.equals(Bool.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in while statement";
            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new InvalidTypeException(msg, type));
        }

        // Process all sub statements recursively
        List<Statement> statements = statement.getStatements().stream().map(this::statement).collect(toList());
        
        return statement.withExpression(expression).withStatements(statements);
    }

    private Expression expression(Expression expression) {
        if (expression instanceof BinaryExpression) {
            Expression left = expression(((BinaryExpression) expression).getLeft());
            Expression right = expression(((BinaryExpression) expression).getRight());
            checkDivisionByZero(expression);

            // If this is a MOD expression involving floats, call library function fmod
            // We cannot check the type of the entire expression, because it has not yet been updated with correct types
            if (expression instanceof ModExpression && (getType(left) instanceof F64 || getType(right) instanceof F64)) {
                expression = functionCall(new FunctionCallExpression(expression.getLine(), expression.getColumn(), FUN_FMOD.getIdentifier(), asList(left, right)));
            } else {
                expression = ((BinaryExpression) expression).withLeft(left).withRight(right);
                checkType((BinaryExpression) expression);
            }
        } else if (expression instanceof FunctionCallExpression) {
            expression = functionCall((FunctionCallExpression) expression);
        } else if (expression instanceof IdentifierDerefExpression) {
            expression = derefExpression((IdentifierDerefExpression) expression);
        } else if (expression instanceof IntegerLiteral) {
            checkInteger((IntegerLiteral) expression);
        } else if (expression instanceof UnaryExpression) {
            Expression subExpr = expression(((UnaryExpression) expression).getExpression());
            expression = ((UnaryExpression) expression).withExpression(subExpr);
            checkType((UnaryExpression) expression);
        }
        return expression;
    }

	private Expression functionCall(FunctionCallExpression expression) {
        // Check and update arguments
        List<Expression> args = expression.getArgs().stream().map(this::expression).collect(toList());
        // Get types of arguments
        List<Type> argTypes = types.getTypes(args);

        // Check that the identifier is a function identifier
        Identifier identifier = expression.getIdentifier();
        String name = identifier.getName();
        if (symbols.containsFunction(name)) {
            try {
                // Match the function with the expected argument types
                Function function = types.resolveFunction(name, argTypes, symbols);
                identifier = function.getIdentifier();
            } catch (SemanticsException e) {
                reportSemanticsError(expression.getLine(), expression.getColumn(), e.getMessage(), e);
            }
        } else {
            String msg = "undefined function: " + name;
            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new UndefinedException(msg, name));
        }

	    return expression.withIdentifier(identifier).withArgs(args);
    }

    private Expression derefExpression(IdentifierDerefExpression ide) {
        String name = ide.getIdentifier().getName();
        if (symbols.contains(name)) {
            // If the identifier is present in the symbol table, reuse that one
            Identifier definedIdentifier = symbols.getIdentifier(name);
            return ide.withIdentifier(definedIdentifier);
        } else if (symbols.containsFunction(name)) {
            // Identifier is a function with no arguments, return a function call expression instead
            Identifier definedIdentifier = symbols.getFunctionIdentifier(name, emptyList());
            return new FunctionCallExpression(ide.getLine(), ide.getColumn(), definedIdentifier, emptyList());
        } else {
            // If the identifier is undefined, make sure it has a type, and add it to the symbol table now
            Identifier identifier = ide.getIdentifier();
            // If the identifier has no type, look it up using type manager
            if (identifier.getType() instanceof Unknown) {
                identifier = identifier.withType(types.getType(ide));
            }
            symbols.addVariable(identifier);

            return ide.withIdentifier(identifier);
        }
    }

    private void checkInteger(IntegerLiteral integer) {
        String value = integer.getValue();
        try {
            Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            String msg = "integer out of range: " + value;
            reportSemanticsError(integer.getLine(), integer.getColumn(), msg, new InvalidException(msg, value));
        }
    }

    private void checkDivisionByZero(Expression expression) {
		if (expression instanceof DivExpression || expression instanceof IDivExpression || expression instanceof ModExpression) {
			Expression right = ((BinaryExpression) expression).getRight();
			if (right instanceof IntegerLiteral) {
				String value = ((IntegerLiteral) right).getValue();
				if (isZero(value)) {
		            String msg = "division by zero: " + value;
		            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new InvalidException(msg, value));
				}
			}
		}
	}

    /**
     * Returns {@code true} if the string {@code value} represents a zero value.
     */
    private boolean isZero(String value) {
        // TODO: Use a regexp to match different zero values.
        return value.equals("0") || value.equals("0.0");
    }

    private void checkType(UnaryExpression expression) {
        Type type = getType(expression.getExpression());
        
        if (expression instanceof ConditionalExpression) {
            // Conditional expressions require subexpression to be boolean
            if (!type.equals(Bool.INSTANCE)) {
                String msg = "expected subexpression of type boolean: " + expression;
                reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new InvalidTypeException(msg, type));
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
                reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new SemanticsException(msg));
            }
        } else if (expression instanceof RelationalExpression) {
            // Relational expressions require both subexpressions to be either strings or numbers
            if (leftType instanceof NumericType && rightType instanceof NumericType) {
                return;
            } else if (leftType instanceof Str && rightType instanceof Str) {
                return;
            } else {
                String msg = "cannot compare " + types.getTypeName(leftType) + " and " + types.getTypeName(rightType);
                reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new SemanticsException(msg));
            }
        } else if (expression instanceof IDivExpression) {
            if (leftType instanceof I64 && rightType instanceof I64) {
                return;
            } else {
                String msg = "expected subexpressions of type integer: " + expression;
                reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new SemanticsException(msg));
            }
        } else {
            getType(expression);
        }
    }

    private Type getType(Expression expression) {
        try {
            return types.getType(expression);
        } catch (SemanticsException se) {
            reportSemanticsError(expression.getLine(), expression.getColumn(), se.getMessage(), se);
            // Return type unknown so we can continue parsing
            return Unknown.INSTANCE;
        }
    }
}
