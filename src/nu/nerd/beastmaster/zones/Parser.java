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
 * Recursive descent parser for Zone Specifications.
 *
 * The EBNF syntax of the Zone Specification Language is as follows:
 *
 * <pre>
 * desc     ::= or-expr EOF
 * or-expr  ::= xor-expr ( '|' xor-expr )*
 * xor-expr ::= and-expr ( '^' and-expr )*
 * and-expr ::= primary ( '&' primary )*
 * primary  ::= pred | '(' or-expr ')' | '!' primary
 * pred     ::= ident '(' ( arg ( ',' arg )* )? ')'
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
        test("biome(\"END_BARRENS\")");
        test("!donut(0,0,500,600) | !y(1,5) ^ biome(\"END_BARRENS\") & wg(\"test\")");
        test("!biome(\"END_BARRENS\") & !wg(\"test\") ^ (circle(1000,1000,200) | circle(500,-500,200))");
    }

    // ------------------------------------------------------------------------
    /**
     * Parse the specified input for informal testing.
     *
     * @param input the Zone Specification to parse.
     */
    static void test(String input) {
        Lexer lexer = null;
        try {
            lexer = new Lexer(input);
            Parser parser = new Parser(lexer);
            Expression expr = parser.parse();

            // Print expression.
            FormatExpressionVisitor debug = new FormatExpressionVisitor();
            StringBuilder sb = new StringBuilder();
            expr.visit(debug, sb);
            System.out.println("Expr: " + sb.toString());

            // Evaluate, with tracing.
            EvalExpressionVisitor eval = new EvalExpressionVisitor(sb);
            sb.setLength(0);
            eval.showBoolean(sb, (Boolean) expr.visit(eval, null));
            System.out.println("Eval: " + sb.toString());

        } catch (ParseError ex) {
            int errorColumn = ex.getToken().getColumn();
            int errorStart = errorColumn - 1;
            int errorEnd = Math.min(input.length(), errorStart + ex.getToken().getLength());
            System.out.println(input.substring(0, errorStart) + ">>>" +
                               input.substring(errorStart, errorEnd) +
                               "<<<" + input.substring(errorEnd));
            System.out.println("\nERROR: column " + errorColumn +
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
     * Return an {@link Expression} representing a Zone Specification in the
     * parsed input.
     *
     * The EBNF syntax rule describing the part of the Zone Specification parsed
     * by this method is:
     *
     * <pre>
     * desc     ::= or-expr EOF
     * </pre>
     *
     * @return an {@link Expression} representing a Zone Specification in the
     *         parsed input.
     */
    public Expression parse() {
        Expression expr = orExpr();
        expect(Token.Type.END);
        return expr;
    }

    // ------------------------------------------------------------------------
    /**
     * The EBNF syntax rule describing the part of the Zone Specification parsed
     * by this method is:
     *
     * <pre>
     * or-expr  ::= xor-expr ( '|' xor-expr )*
     * </pre>
     */
    Expression orExpr() {
        Expression left = xorExpr();
        if (have(Token.Type.OR)) {
            Expression operator = new OrExpression();
            operator.addChild(left);
            while (take(Token.Type.OR)) {
                operator.addChild(xorExpr());
            }
            return operator;
        } else {
            return left;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The EBNF syntax rule describing the part of the Zone Specification parsed
     * by this method is:
     *
     * <pre>
     * xor-expr ::= and-expr ( '^' and-expr )*
     * </pre>
     */
    Expression xorExpr() {
        Expression left = andExpr();
        if (have(Token.Type.XOR)) {
            Expression operator = new XorExpression();
            operator.addChild(left);
            while (take(Token.Type.XOR)) {
                operator.addChild(andExpr());
            }
            return operator;
        } else {
            return left;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The EBNF syntax rule describing the part of the Zone Specification parsed
     * by this method is:
     *
     * <pre>
     * and-expr ::= primary ( '&' primary )*
     * </pre>
     */
    Expression andExpr() {
        Expression left = primary();
        if (have(Token.Type.AND)) {
            Expression operator = new AndExpression();
            operator.addChild(left);
            while (take(Token.Type.AND)) {
                operator.addChild(primary());
            }
            return operator;
        } else {
            return left;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The EBNF syntax rule describing the part of the Zone Specification parsed
     * by this method is:
     *
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
            throw new ParseError("expecting parentheses, ! or a predicate, but got " + _lexer.current(),
                _lexer.current());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The EBNF syntax rule describing the part of the Zone Specification parsed
     * by this method is:
     *
     * <pre>
     * pred     ::= ident '(' ( arg ( ',' arg )* )? ')'
     * arg      ::= number | string
     * </pre>
     *
     * Note that predicates can take zero or more arguments.
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

        int expectedArgCount = zonePred.getParameters().size();
        if (expectedArgCount != argTokens.size()) {
            String plural = (expectedArgCount == 1) ? "" : "s";
            throw new ParseError("expecting " + expectedArgCount +
                                 " predicate argument" + plural + " but got " + argTokens.size(),
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
                throw new ParseError("unexpected end of input (forgot to close your parentheses?)", current);
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