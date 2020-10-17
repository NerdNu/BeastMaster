package nu.nerd.beastmaster.zones;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;

// ----------------------------------------------------------------------------
/**
 * Lexical analyser for Zone specifications.
 */
public class Lexer {
    // ------------------------------------------------------------------------
    /**
     * Main program for informal testing.
     * 
     * @param args
     */
    public static void main(String[] args) {
        Lexer lexer;
        lexer = test("&");
        // Will we keep reading END at end?
        System.out.println(lexer.nextToken());
        test("\"a string\"");
        test("ident");
        test("biome(\"END_BARRENS\") & (circle(1000,1000,200) | circle(500,-500,200))");
        test("00");
        test("\"");
        test(".5");
        test("0.123");
        test("12.3456");
        test("world(\"world\")");
    }

    // ------------------------------------------------------------------------
    /**
     * Print all tokens in the input.
     * 
     * @param input the input.
     */
    static Lexer test(String input) {
        try {
            Lexer lexer = new Lexer(input);
            Token token;
            do {
                token = lexer.next();
                System.out.print(token);
                System.out.print(' ');
            } while (token.getType() != Token.Type.END);
            System.out.println();
            return lexer;
        } catch (ParseError ex) {
            System.out.println("\nERROR: " + ex.getMessage() + " at column " + ex.getColumn());
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param spec the Zone specification.
     */
    public Lexer(String spec) {
        this(new StringReader(spec));
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param spec the Zone specification.
     */
    public Lexer(Reader reader) {
        _reader = new PushbackReader(reader);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the current 1-based column of the most recently read character.
     * 
     * @return the current 1-based column of the most recently read character.
     */
    public int column() {
        return _column;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the most recently read token.
     * 
     * @return the most recently read token.
     */
    public Token current() {
        return (_current == null) ? next() : _current;
    }

    // ------------------------------------------------------------------------
    /**
     * Advance to the next token in the input and return it.
     * 
     * @return the next Token.
     */
    public Token next() {
        _current = nextToken();
        return _current;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the next character of input, or -1 for END.
     * 
     * @return the next character of input or -1 for END.
     * @throws ParseError on I/O error.
     */
    protected int nextChar() {
        try {
            int c = _reader.read();
            ++_column;
            return c;
        } catch (IOException ex) {
            throw new ParseError("unexpected I/O error", _column);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Un-read at most one character of input.
     * 
     * @param c the character to return to the input stream.
     */
    protected void unreadChar(int c) {
        try {
            // PushbackReader doesn't like to push back EOF.
            if (c != -1) {
                _reader.unread(c);
            }
            --_column;
        } catch (IOException ex) {
            throw new ParseError("buffer full pushing back: '" + (char) c + "'", _column);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Read the next token from input.
     * 
     * @return the next TokenType.
     * @throws ParseError on I/O error.
     */
    @SuppressWarnings("deprecation")
    protected Token nextToken() {
        int c;
        do {
            c = nextChar();
        } while (Character.isSpace((char) c));

        switch (c) {
        case '(':
            return new Token(Token.Type.L_PAREN);
        case ')':
            return new Token(Token.Type.R_PAREN);
        case ',':
            return new Token(Token.Type.COMMA);
        case '&':
            return new Token(Token.Type.AND);
        case '|':
            return new Token(Token.Type.OR);
        case '^':
            return new Token(Token.Type.XOR);
        case '!':
            return new Token(Token.Type.NOT);
        case -1:
            return new Token(Token.Type.END);
        }

        if (Character.isLetter(c)) {
            // IDENT
            StringBuilder sb = new StringBuilder();
            do {
                sb.append((char) c);
                c = nextChar();
            } while (Character.isLetter(c));
            unreadChar(c);
            return new Token(Token.Type.IDENT, sb.toString());
        } else if (c == '"') {
            // STRING
            StringBuilder sb = new StringBuilder();
            for (;;) {
                c = nextChar();
                if (c == '"' || c == -1) {
                    break;
                }
                sb.append((char) c);
            }
            if (c == -1) {
                throw new ParseError("unterminated string", column());
            }
            return new Token(Token.Type.STRING, sb.toString());
        } else if (c == '+' || c == '-' || Character.isDigit(c)) {
            // NUMBER
            StringBuilder sb = new StringBuilder();
            if (c == '+') {
                // Don't append '+'.
                c = nextChar();
            } else if (c == '-') {
                sb.append((char) c);
                c = nextChar();
            }

            // Parse integer.
            // Allow a leading zero before decimal point
            int leadingZeroes = 0;
            while (Character.isDigit(c)) {
                if (c == '0') {
                    if (leadingZeroes >= 0) {
                        ++leadingZeroes;
                        if (leadingZeroes > 1) {
                            throw new ParseError("number has too many leading zeroes", _column);
                        }
                    }
                } else {
                    // Stop counting leading zeroes.
                    leadingZeroes = -1;
                }

                sb.append((char) c);
                c = nextChar();
            }

            // Read decimal places?
            if (c == '.') {
                do {
                    sb.append((char) c);
                    c = nextChar();
                } while (Character.isDigit(c));
            }
            unreadChar(c);
            return new Token(Token.Type.NUMBER, sb.toString());
        }

        throw new ParseError("unexpected character: '" + (char) c + "'", _column);
    }

    // ------------------------------------------------------------------------
    /**
     * The Reader that reads characters to form tokens.
     */
    protected PushbackReader _reader;

    /**
     * 1-based column of the next character of input.
     */
    protected int _column;

    /**
     * The most recently read token.
     */
    protected Token _current;
} // class Lexer