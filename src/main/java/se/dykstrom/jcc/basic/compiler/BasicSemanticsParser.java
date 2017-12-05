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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.dykstrom.jcc.basic.ast.OnGotoStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

/**
 * The semantics parser for the Basic language. This parser enforces the semantic rules of the
 * language, including the correct use of line numbers and the type system. It returns a copy
 * of the program, where some types are better defined than in the source program.
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
        } else if (statement instanceof GotoStatement) {
            return gotoStatement((GotoStatement) statement);
        } else if (statement instanceof IfStatement) {
            return ifStatement((IfStatement) statement);
        } else if (statement instanceof OnGotoStatement) {
            return onGotoStatement((OnGotoStatement) statement);
        } else if (statement instanceof PrintStatement) {
            return printStatement((PrintStatement) statement);
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

        // If the identifier was not typed, it derives its type from the expression
        if (identType instanceof Unknown) {
            identType = exprType;
            identifier = identifier.withType(exprType);
        }

        // Save the possibly updated identifier for later
        symbols.addVariable(identifier);

        // Check that expression can be assigned to identifier
        if (!types.isAssignableFrom(identType, exprType)) {
            String msg = "you cannot assign a value of type " + types.getTypeName(exprType)
                    + " to variable '" + name + "' of type " + types.getTypeName(identType);
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new SemanticsException(msg));
        }

        // Return updated statement with the possibly updated identifier and expression
        return statement.withIdentifier(identifier).withExpression(expression);
    }

    private GotoStatement gotoStatement(GotoStatement statement) {
        String line = statement.getGotoLine();
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
            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new SemanticsException(msg));
        }

        // Process all sub statements recursively
        List<Statement> thenStatements = statement.getThenStatements().stream().map(this::statement).collect(toList());
        List<Statement> elseStatements = statement.getElseStatements().stream().map(this::statement).collect(toList());
        
        return statement.withExpression(expression).withThenStatements(thenStatements).withElseStatements(elseStatements);
    }

    private OnGotoStatement onGotoStatement(OnGotoStatement statement) {
        // Check expression
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in on-goto statement";
            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new SemanticsException(msg));
        }

        // Check goto labels
        statement.getGotoLabels().stream()
            .filter(label -> !lineNumbers.contains(label))
            .forEach(label -> {
                String msg = "undefined line number: " + label;
                reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new UndefinedException(msg, label));
            });
        return statement;
    }

    private PrintStatement printStatement(PrintStatement statement) {
        List<Expression> expressions = statement.getExpressions().stream().map(this::expression).collect(toList());
        return statement.withExpressions(expressions);
    }

    private WhileStatement whileStatement(WhileStatement statement) {
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE) && !type.equals(Bool.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in while statement";
            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new SemanticsException(msg));
        }

        // Process all sub statements recursively
        List<Statement> statements = statement.getStatements().stream().map(this::statement).collect(toList());
        
        return statement.withExpression(expression).withStatements(statements);
    }

    private Expression expression(Expression expression) {
        if (expression instanceof BinaryExpression) {
            Expression left = expression(((BinaryExpression) expression).getLeft());
            Expression right = expression(((BinaryExpression) expression).getRight());
            expression = ((BinaryExpression) expression).withLeft(left).withRight(right);
            checkType((BinaryExpression) expression);
            checkDivisionByZero(expression);
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
        List<Type> argTypes = args.stream().map(this::getType).collect(toList());

        // Check that the identifier is a function identifier
        Identifier identifier = expression.getIdentifier();
        String name = identifier.getName();
        if (symbols.containsFunction(name)) {
            try {
                // Match the function with the expected argument types
                identifier = symbols.getFunctionIdentifier(name, argTypes);
            } catch (IllegalArgumentException e) {
                String msg = "no match for function '" + name + "' with arguments " + toString(argTypes);
                reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new InvalidException(msg, name));
            }
        } else {
            String msg = "undefined function: " + name;
            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new UndefinedException(msg, name));
        }

	    return expression.withIdentifier(identifier).withArgs(args);
    }
	
    private String toString(List<Type> argTypes) {
        return argTypes.stream().map(types::getTypeName).collect(joining(", ", "(", ")"));
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
            String msg = "undefined identifier: " + name;
            reportSemanticsError(ide.getLine(), ide.getColumn(), msg, new UndefinedException(msg, name));
            return ide;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
		if (expression instanceof DivExpression) {
			Expression right = ((DivExpression) expression).getRight();
			if (right instanceof IntegerLiteral) {
				String value = ((IntegerLiteral) right).getValue();
				if (value.equals("0")) {
		            String msg = "division by zero: " + value;
		            reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new InvalidException(msg, value));
				}
			}
		}
	}

    private void checkType(UnaryExpression expression) {
        Type type = getType(((NotExpression) expression).getExpression());
        
        if (expression instanceof ConditionalExpression) {
            // Conditional expressions require subexpression to be boolean
            if (!type.equals(Bool.INSTANCE)) {
                String msg = "expected subexpression of type boolean: " + expression;
                reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new SemanticsException(msg));
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
            if (!leftType.equals(Bool.INSTANCE) || !rightType.equals(Bool.INSTANCE)) {
                String msg = "expected subexpressions of type boolean: " + expression;
                reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new SemanticsException(msg));
            }
        } else if (expression instanceof RelationalExpression) {
            // Relational expressions require both subexpressions to be either strings or numbers
            if (leftType instanceof I64 && rightType instanceof I64) {
                return;
            } else if (leftType instanceof Str && rightType instanceof Str) {
                return;
            } else {
                String msg = "cannot compare " + types.getTypeName(leftType) + " and " + types.getTypeName(rightType);
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
