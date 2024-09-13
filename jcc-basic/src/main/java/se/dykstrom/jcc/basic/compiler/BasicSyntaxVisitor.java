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

import se.dykstrom.jcc.basic.ast.*;
import se.dykstrom.jcc.basic.compiler.BasicParser.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.utils.FormatUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static se.dykstrom.jcc.antlr4.Antlr4Utils.isValid;

/**
 * The syntax visitor for the Basic language, used to build an AST from an ANTLR parse tree.
 *
 * @author Johan Dykstrom
 */
@SuppressWarnings("unchecked")
public class BasicSyntaxVisitor extends BasicBaseVisitor<Node> {

    // Group 1 = optional sign
    // Group 2 = complete number
    // Group 3 = decimal point and fraction
    // Group 4 = complete exponent
    // Group 5 = optional exponent sign
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^(-)?(\\d+(\\.\\d*)?|\\.\\d+)([deDE]([-+])?\\d+)?#?$");

    // Group 1 = first letter
    // Group 2 = optional dash and second letter
    // Group 3 = optional second letter
    private static final Pattern LETTER_INTERVAL_PATTERN = Pattern.compile("^([a-zA-Z])(-([a-zA-Z]))*$");

    private final BasicTypeManager typeManager;

    public BasicSyntaxVisitor(BasicTypeManager typeManager) {
        this.typeManager = typeManager;
    }

    @Override
    public Node visitProgram(ProgramContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (LineContext lineCtx : ctx.line()) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            statements.addAll(stmtList.contents());
        }

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new AstProgram(line, column, statements);
    }

    @Override
    public Node visitLine(LineContext ctx) {
        ListNode<Statement> stmtList = (ListNode<Statement>) visitChildren(ctx);
        // Set line number or label on the first statement if available
        if (isValid(ctx.labelOrNumberDef())) {
            String label = getLabel(ctx.labelOrNumberDef());
            return stmtList.withHead(new LabelledStatement(label, stmtList.contents().get(0)));
        }
        return stmtList;
    }

    @Override
    public Node visitStmtList(StmtListContext ctx) {
        List<Statement> statements = new ArrayList<>();

        if (isValid(ctx.stmtList())) {
            ListNode<Statement> stmtList = (ListNode<Statement>) ctx.stmtList().accept(this);
            statements.addAll(stmtList.contents());
        }
        statements.add((Statement) ctx.stmt().accept(this));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, statements);
    }

    @Override
    public Node visitAssignStmt(AssignStmtContext ctx) {
        IdentifierExpression lhsExpression = (IdentifierExpression) ctx.identExpr().accept(this);
        Expression rhsExpression = (Expression) ctx.expr().accept(this);

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new AssignStatement(line, column, lhsExpression, rhsExpression);
    }

    @Override
    public Node visitIdentExpr(IdentExprContext ctx) {
        if (isValid(ctx.arrayElement())) {
            return ctx.arrayElement().accept(this);
        } else {
            IdentifierExpression ie = (IdentifierExpression) ctx.ident().accept(this);
            return IdentifierNameExpression.from(ie, ie.getIdentifier());
        }
    }

    @Override
    public Node visitClsStmt(ClsStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ClsStatement(line, column);
    }

    @Override
    public Node visitConstStmt(ConstStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        List<DeclarationAssignment> declarations = ctx.constDecl().stream()
                .map(c -> (DeclarationAssignment) c.accept(this))
                .toList();
        return new ConstDeclarationStatement(line, column, declarations);
    }

    @Override
    public Node visitConstDecl(ConstDeclContext ctx) {
        final int line = ctx.getStart().getLine();
        final int column = ctx.getStart().getCharPositionInLine();
        final String name = ctx.ident().getText();
        final Expression expression = (Expression) ctx.expr().accept(this);
        return new DeclarationAssignment(line, column, name, null, expression);
    }

    @Override
    public Node visitCommentStmt(CommentStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new CommentStatement(line, column);
    }

    @Override
    public Node visitDefFnStmt(DefFnStmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final int column = ctx.getStart().getCharPositionInLine();
        final var identifier = ((IdentifierExpression) ctx.ident().accept(this));
        final var expression = (Expression) ctx.expr().accept(this);
        final var declarations = ctx.paramDecl().stream()
                .map(c -> c.accept(this))
                .map(Declaration.class::cast)
                .toList();
        final var argTypes = declarations.stream().map(Declaration::type).toList();

        final var functionType = Fun.from(argTypes, identifier.getType());
        final var functionIdentifier = identifier.getIdentifier().withType(functionType);
        return new FunctionDefinitionStatement(line, column, functionIdentifier, declarations, expression);
    }

    @Override
    public Node visitParamDecl(ParamDeclContext ctx) {
        final int line = ctx.getStart().getLine();
        final int column = ctx.getStart().getCharPositionInLine();
        final var identifier = ((IdentifierExpression) ctx.ident().accept(this)).getIdentifier();
        final Type type;
        if (isValid(ctx.TYPE_DOUBLE())) {
            type = F64.INSTANCE;
        } else if (isValid(ctx.TYPE_INTEGER())) {
            type = I64.INSTANCE;
        } else if (isValid(ctx.TYPE_STRING())) {
            type = Str.INSTANCE;
        } else {
            type = identifier.type();
        }
        return new Declaration(line, column, identifier.name(), type);
    }

    @Override
    public Node visitDefTypeStmt(DefTypeStmtContext ctx) {
        ListNode<Character> letterList = (ListNode<Character>) ctx.letterList().accept(this);
        Set<Character> letters = new HashSet<>(letterList.contents());
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();

        if (isValid(ctx.DEFDBL())) {
            typeManager.defineTypeByName(letters, F64.INSTANCE);
            return new DefDblStatement(line, column, letters);
        } else if (isValid(ctx.DEFINT())) {
            typeManager.defineTypeByName(letters, I64.INSTANCE);
            return new DefIntStatement(line, column, letters);
        } else {
            typeManager.defineTypeByName(letters, Str.INSTANCE);
            return new DefStrStatement(line, column, letters);
        }
    }

    @Override
    public Node visitLetterList(LetterListContext ctx) {
        List<Character> letters = new ArrayList<>();
        if (isValid(ctx.letterList())) {
            ListNode<Character> letterList = (ListNode<Character>) ctx.letterList().accept(this);
            letters.addAll(letterList.contents());
        }
        letters.addAll(((ListNode<Character>) ctx.letterInterval().accept(this)).contents());

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, letters);
    }

    @Override
    public Node visitLetterInterval(LetterIntervalContext ctx) {
        Matcher matcher = LETTER_INTERVAL_PATTERN.matcher(ctx.getText());
        if (matcher.matches()) {
            String start = matcher.group(1);
            String end = matcher.group(3);

            // Interval end is optional
            if (end == null) {
                end = start;
            }

            // Expand letter interval to a list of characters
            List<Character> letters = new ArrayList<>();
            for (char c = start.charAt(0); c <= end.charAt(0); c++) {
                letters.add(c);
            }

            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            return new ListNode<>(line, column, letters);
        }

        throw new IllegalArgumentException("invalid letter interval: " + ctx.getText());
    }

    @Override
    public Node visitDimStmt(DimStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        List<Declaration> declarations = ctx.varDecl().stream()
                .map(c -> (Declaration) c.accept(this))
                .toList();
        return new VariableDeclarationStatement(line, column, declarations);
    }

    @Override
    public Node visitVarDecl(VarDeclContext ctx) {
        Type type;
        if (isValid(ctx.TYPE_DOUBLE())) {
            type = F64.INSTANCE;
        } else if (isValid(ctx.TYPE_INTEGER())) {
            type = I64.INSTANCE;
        } else if (isValid(ctx.TYPE_STRING())) {
            type = Str.INSTANCE;
        } else {
            throw new IllegalArgumentException("unknown type: " + ctx.getText());
        }

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        String name = ctx.ident().getText();

        // If this is an array declaration, find out its dimensions and subscripts
        if (!ctx.subscriptDecl().isEmpty()) {
            List<Expression> subscripts = ctx.subscriptDecl().stream()
                    .map(c -> (Expression) c.accept(this))
                    .toList();
            Arr arrayType = Arr.from(subscripts.size(), type);
            return new ArrayDeclaration(line, column, name, arrayType, subscripts);
        } else {
            return new Declaration(line, column, name, type);
        }
    }

    @Override
    public Node visitSubscriptDecl(SubscriptDeclContext ctx) {
        return ctx.addSubExpr().accept(this);
    }

    @Override
    public Node visitEndStmt(EndStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new EndStatement(line, column);
    }

    @Override
    public Node visitGosubStmt(GosubStmtContext ctx) {
        String label = ctx.labelOrNumber().getText();
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new GosubStatement(line, column, label);
    }

    @Override
    public Node visitReturnStmt(ReturnStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ReturnStatement(line, column);
    }

    @Override
    public Node visitSleepStmt(SleepStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final Expression expression;
        if (isValid(ctx.addSubExpr())) {
            expression = (Expression) ctx.addSubExpr().accept(this);
        } else {
            expression = null;
        }
        return new SleepStatement(line, column, expression);
    }

    @Override
    public Node visitRandomizeStmt(RandomizeStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        Expression expression = null;
        if (isValid(ctx.addSubExpr())) {
            expression = (Expression) ctx.addSubExpr().accept(this);
        }
        return new RandomizeStatement(line, column, expression);
    }

    @Override
    public Node visitSwapStmt(SwapStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        IdentifierExpression first = (IdentifierExpression) ctx.identExpr(0).accept(this);
        IdentifierExpression second = (IdentifierExpression) ctx.identExpr(1).accept(this);
        return new SwapStatement(line, column, first, second);
    }

    @Override
    public Node visitSystemStmt(SystemStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new SystemStatement(line, column);
    }

    @Override
    public Node visitLineInputStmt(LineInputStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        IdentifierExpression ie = (IdentifierExpression) ctx.ident().accept(this);
        boolean inhibitNewline = isValid(ctx.SEMICOLON());
        String prompt = isValid(ctx.prompt()) ? getPrompt(ctx.prompt()) : null;
        return LineInputStatement.builder(ie.getIdentifier())
                .line(line)
                .column(column)
                .inhibitNewline(inhibitNewline)
                .prompt(prompt)
                .build();
    }

    @Override
    public Node visitGotoStmt(GotoStmtContext ctx) {
        String label = getLabel(ctx.labelOrNumber());
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new GotoStatement(line, column, label);
    }

    @Override
    public Node visitOnGosubStmt(OnGosubStmtContext ctx) {
        List<String> labels = new ArrayList<>();
        if (isValid(ctx.labelOrNumberList())) {
            ListNode<String> labelList = (ListNode<String>) ctx.labelOrNumberList().accept(this);
            labels.addAll(labelList.contents());
        }
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        Expression expression = (Expression) ctx.expr().accept(this);
        return new OnGosubStatement(line, column, expression, labels);
    }

    @Override
    public Node visitOnGotoStmt(OnGotoStmtContext ctx) {
        List<String> labels = new ArrayList<>();
        if (isValid(ctx.labelOrNumberList())) {
            ListNode<String> labelList = (ListNode<String>) ctx.labelOrNumberList().accept(this);
            labels.addAll(labelList.contents());
        }
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        Expression expression = (Expression) ctx.expr().accept(this);
        return new OnGotoStatement(line, column, expression, labels);
    }

    @Override
    public Node visitLabelOrNumberList(LabelOrNumberListContext ctx) {
        List<String> labels = new ArrayList<>();
        if (isValid(ctx.labelOrNumberList())) {
            ListNode<String> labelList = (ListNode<String>) ctx.labelOrNumberList().accept(this);
            labels.addAll(labelList.contents());
        }
        labels.add(getLabel(ctx.labelOrNumber()));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, labels);
    }

    @Override
    public Node visitOptionBaseStmt(OptionBaseStmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final int column = ctx.getStart().getCharPositionInLine();
        final int base = Integer.parseInt(ctx.NUMBER().getText());
        return new OptionBaseStatement(line, column, base);
    }

    @Override
    public Node visitPrintStmt(PrintStmtContext ctx) {
        List<Expression> expressions = new ArrayList<>();
        if (isValid(ctx.printList())) {
            ListNode<Expression> printList = (ListNode<Expression>) ctx.printList().accept(this);
            expressions.addAll(printList.contents());
        }

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new PrintStatement(line, column, expressions);
    }

    @Override
    public Node visitPrintList(PrintListContext ctx) {
        List<Expression> expressions = new ArrayList<>();
        if (isValid(ctx.printList())) {
            ListNode<Expression> printList = (ListNode<Expression>) ctx.printList().accept(this);
            expressions.addAll(printList.contents());
        }
        expressions.add((Expression) ctx.expr().accept(this));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, expressions);
    }

    // If statements:

    @Override
    public Node visitIfGoto(IfGotoContext ctx) {
        Expression expression = (Expression) ctx.expr().accept(this);
        int gotoLine = ctx.GOTO().getSymbol().getLine();
        int gotoColumn = ctx.GOTO().getSymbol().getCharPositionInLine();
        String gotoLabel = getLabel(ctx.labelOrNumber());
        List<Statement> thenStatements = List.of(new GotoStatement(gotoLine, gotoColumn, gotoLabel));

        List<Statement> elseStatements = parseSingleLineElse(ctx.elseSingle());
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return IfStatement.builder(expression, thenStatements).elseStatements(elseStatements).line(line).column(column).build();
    }

    @Override
    public Node visitIfThenSingle(IfThenSingleContext ctx) {
        Expression expression = (Expression) ctx.expr().accept(this);
        
        List<Statement> thenStatements;
        if (isValid(ctx.labelOrNumber())) {
            int gotoLine = ctx.THEN().getSymbol().getLine();
            int gotoColumn = ctx.THEN().getSymbol().getCharPositionInLine();
            String gotoLabel = getLabel(ctx.labelOrNumber());
            thenStatements = List.of(new GotoStatement(gotoLine, gotoColumn, gotoLabel));
        } else {
            ListNode<Statement> stmtList = (ListNode<Statement>) ctx.stmtList().accept(this);
            thenStatements = stmtList.contents();
        }
        
        List<Statement> elseStatements = parseSingleLineElse(ctx.elseSingle());
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return IfStatement.builder(expression, thenStatements).elseStatements(elseStatements).line(line).column(column).build();
    }

    private List<Statement> parseSingleLineElse(ElseSingleContext elseCtx) {
        if (isValid(elseCtx)) {
            if (isValid(elseCtx.labelOrNumber())) {
                int line = elseCtx.ELSE().getSymbol().getLine();
                int column = elseCtx.ELSE().getSymbol().getCharPositionInLine();
                String label = getLabel(elseCtx.labelOrNumber());
                return List.of(new GotoStatement(line, column, label));
            } else {
                ListNode<Statement> stmtList = (ListNode<Statement>) elseCtx.accept(this);
                return stmtList.contents();
            }
        } else {
            return List.of();
        }
    }

    @Override
    public Node visitIfThenBlock(IfThenBlockContext ctx) {
        // IF expr THEN 
        //   statements 
        // ELSEIF expr THEN 
        //   statements 
        // ELSE 
        //   statements 
        // END IF
        
        List<Statement> thenStatements = new ArrayList<>();
        List<Statement> elseStatements = new ArrayList<>();

        // Visit the parts in reverse order:
        
        // END IF
        if (isValid(ctx.endIf())) {
            final var endIfCtx = ctx.endIf();
            if (isValid(endIfCtx.labelOrNumberDef())) {
                final int line = endIfCtx.getStart().getLine();
                final int column = endIfCtx.getStart().getCharPositionInLine();
                final String label = getLabel(endIfCtx.labelOrNumberDef());
                // If there is a line number before END IF, add a comment just to preserve the line number
                elseStatements.add(new LabelledStatement(label, new CommentStatement(line, column, "END IF")));
            }
        }
        
        // ELSE block
        if (isValid(ctx.elseBlock())) {
            final var elseCtx = ctx.elseBlock();
            elseStatements.addAll(0, parseBlock(elseCtx.line()));
            if (isValid(elseCtx.labelOrNumberDef())) {
                final int line = elseCtx.getStart().getLine();
                final int column = elseCtx.getStart().getCharPositionInLine();
                final String label = getLabel(elseCtx.labelOrNumberDef());
                // If there is a line number before ELSE, add a comment just to preserve the line number
                elseStatements.add(0, new LabelledStatement(label, new CommentStatement(line, column, "ELSE")));
            }
        }

        // ELSEIF blocks
        for (int i = ctx.elseIfBlock().size() - 1; i >= 0; i--) {
            ElseIfBlockContext elseIfCtx = ctx.elseIfBlock(i);
            Expression elseIfExpression = (Expression) elseIfCtx.expr().accept(this);
            ListNode<Statement> stmtList = (ListNode<Statement>) elseIfCtx.accept(this);
            List<Statement> elseIfStatements = stmtList.contents();
            
            int line = elseIfCtx.getStart().getLine();
            int column = elseIfCtx.getStart().getCharPositionInLine();
            elseStatements = List.of(IfStatement.builder(elseIfExpression, elseIfStatements)
                    .elseStatements(elseStatements)
                    .line(line)
                    .column(column)
                    .build());
        }

        // THEN block
        for (LineContext lineCtx : ctx.line()) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            thenStatements.addAll(stmtList.contents());
        }
        
        Expression ifExpression = (Expression) ctx.expr().accept(this);
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return IfStatement.builder(ifExpression, thenStatements).elseStatements(elseStatements).line(line).column(column).build();
    }

    @Override
    public Node visitElseIfBlock(ElseIfBlockContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        
        List<Statement> statements = new ArrayList<>();
        if (isValid(ctx.labelOrNumberDef())) {
            // If there is a line number before ELSEIF, add a comment just to preserve the line number
            final var label = getLabel(ctx.labelOrNumberDef());
            statements.add(new LabelledStatement(label, new CommentStatement(line, column, "ELSEIF")));
        }
        statements.addAll(parseBlock(ctx.line()));

        return new ListNode<>(line, column, statements);
    }

    // While statements:

    @Override
    public Node visitWhileStmt(WhileStmtContext ctx) {
        // WHILE expr 
        //   statements 
        // WEND
        
        List<Statement> statements = new ArrayList<>();

        // Visit the parts in reverse order:
        
        // WEND
        if (isValid(ctx.labelOrNumberDef())) {
            final int line = ctx.WEND().getSymbol().getLine();
            final int column = ctx.WEND().getSymbol().getCharPositionInLine();
            final String label = getLabel(ctx.labelOrNumberDef());
            statements.add(new LabelledStatement(label, new CommentStatement(line, column, "WEND")));
        }

        // WHILE block
        statements.addAll(0, parseBlock(ctx.line()));
        
        Expression expression = (Expression) ctx.expr().accept(this);
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new WhileStatement(line, column, expression, statements);
    }

    /**
     * Parses a block of statements, that is, a number of lines, and returns the result as a list of statements.
     */
    private List<Statement> parseBlock(List<LineContext> block) {
        List<Statement> statements = new ArrayList<>();
        for (LineContext lineCtx : block) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            statements.addAll(stmtList.contents());
        }
        return statements;
    }    
    
    // Expressions:
    
    @Override
    public Node visitOrExpr(OrExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.orExpr().accept(this);
            final var right = (Expression) ctx.andExpr().accept(this);

            if (isValid(ctx.EQV())) {
                return new EqvExpression(line, column, left, right);
            } else if (isValid(ctx.IMP())) {
                return new ImpExpression(line, column, left, right);
            } else if (isValid(ctx.OR())) {
                return new OrExpression(line, column, left, right);
            } else { // ctx.XOR()
                return new XorExpression(line, column, left, right);
            }
        }
    }

    @Override
    public Node visitAndExpr(AndExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.andExpr().accept(this);
            Expression right = (Expression) ctx.notExpr().accept(this);

            return new AndExpression(line, column, left, right);
        }
    }

    @Override
    public Node visitNotExpr(NotExprContext ctx) {
        if (isValid(ctx.NOT())) {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression expression = (Expression) ctx.relExpr().accept(this);
            return new NotExpression(line, column, expression);
        } else {
            return visitChildren(ctx);
        }
    }

    @Override
    public Node visitRelExpr(RelExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.addSubExpr(0).accept(this);
            Expression right = (Expression) ctx.addSubExpr(1).accept(this);

            if (isValid(ctx.EQ())) {
                return new EqualExpression(line, column, left, right);
            } else if (isValid(ctx.GE())) {
                return new GreaterOrEqualExpression(line, column, left, right);
            } else if (isValid(ctx.GT())) {
                return new GreaterExpression(line, column, left, right);
            } else if (isValid(ctx.LE())) {
                return new LessOrEqualExpression(line, column, left, right);
            } else if (isValid(ctx.LT())) {
                return new LessExpression(line, column, left, right);
            } else { // ctx.NE()
                return new NotEqualExpression(line, column, left, right);
            }
        }
    }

    @Override
    public Node visitAddSubExpr(AddSubExprContext ctx) {
        Node expr;
        if (ctx.getChildCount() == 1) {
            expr = visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.addSubExpr().accept(this);
            Expression right = (Expression) ctx.term().accept(this);

            if (isValid(ctx.PLUS())) {
                expr = new AddExpression(line, column, left, right);
            } else { // ctx.MINUS()
                expr = new SubExpression(line, column, left, right);
            }
        }
        return expr;
    }

    @Override
    public Node visitTerm(TermContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.term().accept(this);
            final var right = (Expression) ctx.factor().accept(this);

            if (isValid(ctx.STAR())) {
                return new MulExpression(line, column, left, right);
            } else if (isValid(ctx.SLASH())) {
                return new DivExpression(line, column, left, right);
            } else if (isValid(ctx.BACKSLASH())) {
                return new IDivExpression(line, column, left, right);
            } else {
                return new ModExpression(line, column, left, right);
            }
        }
    }

    @Override
    public Node visitFactor(FactorContext ctx) {
        if (isValid(ctx.CIRCUMFLEX())) {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.factor(0).accept(this);
            Expression right = (Expression) ctx.factor(1).accept(this);
            return new ExpExpression(line, column, left, right);
        } else if (isValid(ctx.MINUS())) {
            Expression expression = (Expression) ctx.factor(0).accept(this);
            if (expression instanceof IntegerLiteral integer) {
                // For negative integer literals, we can just update the value
                return integer.withValue("-" + integer.getValue());
            } else if (expression instanceof FloatLiteral floating) {
                // And for negative float literals, the same
                return floating.withValue("-" + floating.getValue());
            } else {
                // For other expressions, we have to construct a negation expression
                int line = ctx.getStart().getLine();
                int column = ctx.getStart().getCharPositionInLine();
                return new NegateExpression(line, column, expression);
            }
        } else if (isSubExpression(ctx)) {
            return ctx.expr().accept(this);
        } else {
            Node factor = visitChildren(ctx);
            if (factor instanceof IdentifierExpression identifierExpression) {
                factor = IdentifierDerefExpression.from(identifierExpression);
            }
            return factor;
        }
    }

    @Override
    public Node visitFunctionCall(FunctionCallContext ctx) {
        IdentifierExpression identifier = (IdentifierExpression) ctx.ident().accept(this);
        List<Expression> expressions = ctx.expr().stream()
                .map(c -> (Expression) c.accept(this))
                .toList();
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        // We know the identifier is a function, and we know its return type,
        // but we do not yet know the argument types
        final var functionType = Fun.from(List.of(), identifier.getType());
        final var functionIdentifier = identifier.getIdentifier().withType(functionType);
        return new FunctionCallExpression(line, column, functionIdentifier, expressions);
    }

    @Override
    public Node visitArrayElement(ArrayElementContext ctx) {
        Identifier identifier = ((IdentifierExpression) ctx.ident().accept(this)).getIdentifier();
        List<Expression> subscripts = ctx.subscriptDecl().stream()
                .map(c -> (Expression) c.accept(this))
                .toList();
        Arr arrayType = Arr.from(subscripts.size(), identifier.type());
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ArrayAccessExpression(line, column, identifier.withType(arrayType), subscripts);
    }

    @Override
    public Node visitString(StringContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        String text = ctx.getText();
        return new StringLiteral(line, column, text.substring(1, text.length() - 1));
    }

    @Override
    public Node visitFloating(FloatingContext ctx) {
        final Matcher matcher = FLOAT_PATTERN.matcher(ctx.getText().trim());
        if (matcher.matches()) {
            final String normalizedNumber = FormatUtils.normalizeFloatNumber(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(4),
                    matcher.group(5),
                    "e"
            );
            final int line = ctx.getStart().getLine();
            final int column = ctx.getStart().getCharPositionInLine();
            return new FloatLiteral(line, column, normalizedNumber);
        } else {
            throw new IllegalArgumentException("Input '" + ctx.getText().trim() + "' failed to match regexp");
        }
    }

    @Override
    public Node visitInteger(IntegerContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        if (isValid(ctx.NUMBER())) {
            return new IntegerLiteral(line, column, ctx.NUMBER().getText());
        } else if (isValid(ctx.HEXNUMBER())) {
            String hex = ctx.HEXNUMBER().getText().substring(2);
            long value = Long.parseLong(hex, 16);
            return new IntegerLiteral(line, column, value);
        } else if (isValid(ctx.OCTNUMBER())) {
            String oct = ctx.OCTNUMBER().getText().substring(2);
            long value = Long.parseLong(oct, 8);
            return new IntegerLiteral(line, column, value);
        } else {
            String bin = ctx.BINNUMBER().getText().substring(2);
            long value = Long.parseLong(bin, 2);
            return new IntegerLiteral(line, column, value);
        }
    }

    @Override
    public Node visitIdent(IdentContext ctx) {
        final int line = ctx.getStart().getLine();
        final int column = ctx.getStart().getCharPositionInLine();
        final String name = ctx.getText();
        final Optional<Type> optionalType = typeManager.getTypeByTypeSpecifier(name);
        final Type type = optionalType.or(() -> typeManager.getTypeByName(name)).orElse(F64.INSTANCE);
        return new IdentifierExpression(line, column, new Identifier(name, type));
    }

    /**
     * Returns the actual prompt from a prompt context.
     */
    private static String getPrompt(PromptContext promptCtx) {
        // Assume that context specifies a valid prompt
        String text = promptCtx.getText().trim();
        return text.substring(1, text.length() - 2);
    }

    /**
     * Returns the actual label (or line number) from a label definition context.
     */
    private static String getLabel(LabelOrNumberDefContext labelCtx) {
        if (isValid(labelCtx.NUMBER())) {
            return labelCtx.NUMBER().getText();
        } else if (isValid(labelCtx.ID())) {
            return labelCtx.ID().getText();
        }
        return null;
    }

    /**
     * Returns the actual label (or line number) from a label context.
     */
    private static String getLabel(LabelOrNumberContext labelCtx) {
        if (isValid(labelCtx.NUMBER())) {
            return labelCtx.NUMBER().getText();
        } else if (isValid(labelCtx.ID())) {
            return labelCtx.ID().getText();
        }
        return null;
    }

    /**
     * Returns {@code true} if the given factor is a subexpression.
     */
    private static boolean isSubExpression(FactorContext factor) {
        return isValid(factor.OPEN()) && isValid(factor.CLOSE());
    }
}
