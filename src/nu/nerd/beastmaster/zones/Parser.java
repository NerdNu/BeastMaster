package nu.nerd.beastmaster.zones;

import java.io.PrintWriter;

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
        test("biome(\"END_BARRENS\")");
        test("!circle(0,0,500) | !y(1,5) ^ biome(\"END_BARRENS\") & region(\"test\")");
        test("biome(\"END_BARRENS\") & !wg(\"test\") ^ (circle(1000,1000,200) | circle(500,-500,200))");
    }

    // ------------------------------------------------------------------------

    static void test(String input) {
        PrintWriter pw = new PrintWriter(System.out, true);
        DebugExpressionVisitor visitor = new DebugExpressionVisitor(pw);
        Lexer lexer = null;
        try {
            lexer = new Lexer(input);
            Parser parser = new Parser(lexer);
            Expression expr = parser.parse();
            expr.visit(visitor);
            pw.println();
        } catch (ParseError ex) {
            System.out.println("\nERROR: " + ex.getMessage() + " at column " + ex.getColumn());
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
                _lexer.current().getColumn());
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
        Expression pred = new PredicateExpression(ident.getText());
        expect(Token.Type.L_PAREN);
        for (;;) {
            if (have(Token.Type.NUMBER)) {
                Token number = take();
                pred.addChild(new NumberExpression(number.getDouble()));
            } else if (have(Token.Type.STRING)) {
                Token string = take();
                pred.addChild(new StringExpression(string.getText()));
            } else {
                if (!take(Token.Type.COMMA)) {
                    break;
                }
            }
        }

        expect(Token.Type.R_PAREN);
        return pred;
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
            throw new ParseError("got " + _lexer.current() + " when expecting " + tokenType,
                _lexer.current().getColumn());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The Lexer.
     */
    protected Lexer _lexer;
} // class Parser