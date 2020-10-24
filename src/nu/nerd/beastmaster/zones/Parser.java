package nu.nerd.beastmaster.zones;

import java.util.ArrayList;
import java.util.List;

import nu.nerd.beastmaster.zones.nodes.AndExpression;
import nu.nerd.beastmaster.zones.nodes.NotExpression;
import nu.nerd.beastmaster.zones.nodes.NumberExpression;
import nu.nerd.beastmaster.zones.nodes.OrExpression;
import nu.nerd.beastmaster.zones.nodes.PredicateExpression;
import nu.nerd.beastmaster.zones.nodes.StringExpression;
import nu.nerd.beastmaster.zones.nodes.XorExpression;

// ----------------------------------------------------------------------------
/**
 * Parser for Zone Specifications.
 * 
 * <pre>
 * desc     ::= or-expr EOF
 * or-expr  ::= xor-expr ( '|' xor-expr )*
 * xor-expr ::= and-expr ( '^' and-expr )*
 * and-expr ::= primary ( '&' primary )*
 * primary  ::= pred | '(' or-expr ')' | '!' primary
 * pred     ::= ident '(' arg ( ',' arg )* ')'
 * arg      ::= number | string
 * </pre>
 */
public class Parser {
    // ------------------------------------------------------------------------
    /**
     * Main program for informal testing.
     * 
     * @param args
     */
    public static void main(String[] args) {
        test("circle(0,0,500)");
        test("biome(\"END_BARRENS\") &");
        test("!donut(0,0,500,600) | !y(1,5) ^ biome(\"END_BARRENS\") & wg(\"test\")");
        test("!biome(\"END_BARRENS\") & !wg(\"test\") ^ (circle(1000,1000,200) | circle(500,-500,200))");
    }

    // ------------------------------------------------------------------------

    static void test(String input) {
        Lexer lexer = null;
        try {
            lexer = new Lexer(input);
            Parser parser = new Parser(lexer);
            Expression expr = parser.parse();

            // Print expression.
            DebugExpressionVisitor debug = new DebugExpressionVisitor();
            StringBuilder sb = new StringBuilder();
            expr.visit(debug, sb);
            System.out.println("Expr: " + sb.toString());

            // Evaluate, with tracing.
            EvalExpressionVisitor eval = new EvalExpressionVisitor(sb);
            sb.setLength(0);
            eval.showBoolean(sb, (Boolean) expr.visit(eval, null));
            System.out.println("Eval: " + sb.toString());

        } catch (ParseError ex) {
            int errorStart = ex.getToken().getColumn() - 1;
            int errorEnd = Math.min(input.length(), errorStart + ex.getToken().getLength());
            System.out.println(input.substring(0, errorStart) + ">>>" +
                               input.substring(errorStart, errorEnd) +
                               "<<<" + input.substring(errorEnd));
            System.out.println("\nERROR: column " + errorStart +
                               ": " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param lexer the Lexer used to extract tokens from the input.
     */
    public Parser(Lexer lexer) {
        _lexer = lexer;
    }

    // ------------------------------------------------------------------------
    /**
     * <pre>
     * desc     ::= or-expr EOF
     * </pre>
     */
    public Expression parse() {
        Expression expr = orExpr();
        expect(Token.Type.END);
        return expr;
    }

    // ------------------------------------------------------------------------
    /**
     * <pre>
     * or-expr  ::= xor-expr ( '|' xor-expr )*
     * </pre>
     */
    Expression orExpr() {
        Expression left = xorExpr();
        if (take(Token.Type.OR)) {
            Expression right = xorExpr();
            Expression operator = new OrExpression();
            operator.addChild(left);
            operator.addChild(right);
            return operator;
        } else {
            return left;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * <pre>
     * xor-expr ::= and-expr ( '^' and-expr )*
     * </pre>
     */
    Expression xorExpr() {
        Expression left = andExpr();
        if (take(Token.Type.XOR)) {
            Expression right = andExpr();
            Expression operator = new XorExpression();
            operator.addChild(left);
            operator.addChild(right);
            return operator;
        } else {
            return left;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * <pre>
     * and-expr ::= primary ( '&' primary )*
     * </pre>
     */
    Expression andExpr() {
        Expression left = primary();
        if (take(Token.Type.AND)) {
            Expression right = primary();
            Expression operator = new AndExpression();
            operator.addChild(left);
            operator.addChild(right);
            return operator;
        } else {
            return left;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * <pre>
     * primary  ::= pred | '(' or-expr ')' | '!' primary
     * </pre>
     */
    Expression primary() {
        if (have(Token.Type.IDENT)) {
            // Don't consume IDENT here. Let predicate() do it.
            return predicate();
        } else if (take(Token.Type.L_PAREN)) {
            Expression expr = orExpr();
            expect(Token.Type.R_PAREN);
            return expr;
        } else if (take(Token.Type.NOT)) {
            Expression not = new NotExpression();
            not.addChild(primary());
            return not;
        } else {
            throw new ParseError("expecting parentheses, ! or a predicate, but got " +
                                 _lexer.current().getType(),
                _lexer.current());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Allow zero-arg predicates.
     * 
     * <pre>
     * pred     ::= ident '(' ( arg ( ',' arg )* )? ')'
     * arg      ::= number | string
     * </pre>
     */
    Expression predicate() {
        Token ident = expect(Token.Type.IDENT);
        PredicateExpression predExpr = new PredicateExpression(ident.getText());
        expect(Token.Type.L_PAREN);

        List<Token> argTokens = new ArrayList<>();
        do {
            if (have(Token.Type.NUMBER) || have(Token.Type.STRING)) {
                argTokens.add(take());
            }
        } while (take(Token.Type.COMMA));

        if (have(Token.Type.IDENT)) {
            // Don't call take() so we avoid parsing next character, e.g.
            // '_' in END_BARRENS.
            Token unexpected = _lexer.current();
            throw new ParseError("got an identifier when expecting a string or number (did you forget to put double quotes around a string?)",
                unexpected);
        }
        Token rParen = expect(Token.Type.R_PAREN);

        // Validate number and types of arguments before expecting the R_PAREN
        // for a more informative error message.
        ZonePredicate zonePred = ZonePredicate.byIdent(predExpr.getIdent());
        if (zonePred == null) {
            // TODO: replace ParseError with a call to an error() function with
            // an error ID to allow us to show suggestions for predicate names.
            throw new ParseError("unknown predicate \"" + predExpr.getIdent() + "\"", ident);
        }
        if (zonePred.getParameters().size() != argTokens.size()) {
            throw new ParseError("expecting " + zonePred.getParameters().size() +
                                 " predicate arguments but got " + argTokens.size(),
                rParen);
        }
        zonePred.getParameters().validateTypes(argTokens);

        for (int i = 0; i < argTokens.size(); ++i) {
            Token token = argTokens.get(i);
            if (token.getType() == Token.Type.NUMBER) {
                predExpr.addChild(new NumberExpression(token.getDouble()));
            } else { // (token.getType() == Token.Type.STRING)
                predExpr.addChild(new StringExpression((String) token.getValue()));
            }
            predExpr.args.add(token.getValue());
        }

        try {
            zonePred.validateArgs(argTokens, predExpr.args);
        } catch (ParseError ex) {
            throw new ParseError("invalid arguments to " + predExpr.getIdent() +
                                 "(): " + ex.getMessage(),
                ex.getToken());
        }

        return predExpr;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the current token has the specified type.
     * 
     * @param tokenType the type of the token.
     * @return true if the current token matched.
     */
    boolean have(Token.Type tokenType) {
        return (_lexer.current().getType() == tokenType);
    }

    // ------------------------------------------------------------------------
    /**
     * If the current token has the specified type, advance to the next Token
     * and return true.
     * 
     * Otherwise, don't advance and return false.
     *
     * @param tokenType the type of Token to take.
     * @return true if the current token matched.
     */
    boolean take(Token.Type tokenType) {
        if (have(tokenType)) {
            _lexer.next();
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the current token and advance the Lexer to the next token.
     * 
     * @return the current token.
     */
    Token take() {
        Token token = _lexer.current();
        _lexer.next();
        return token;
    }

    // ------------------------------------------------------------------------
    /**
     * Throw a ParseError if the current token is not of the expected type.
     * 
     * @return the expected Token and advance to the next Token.
     * @throws ParseError if the expected Token cannot be taken.
     */
    Token expect(Token.Type tokenType) {
        Token token = _lexer.current();
        if (take(tokenType)) {
            return token;
        } else {
            Token current = _lexer.current();
            if (current.getType() == Token.Type.END) {
                throw new ParseError("unexpected end of input (forgot to close yor parentheses?)", current);
            }

            throw new ParseError("got " + current + " when expecting " + tokenType.asExpected(), current);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The Lexer.
     */
    protected Lexer _lexer;
} // class Parser