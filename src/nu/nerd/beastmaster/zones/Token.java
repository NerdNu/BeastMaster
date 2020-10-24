package nu.nerd.beastmaster.zones;

// ----------------------------------------------------------------------------
/**
 * Represents a lexical token in Zone specifications.
 */
public class Token {
    // ------------------------------------------------------------------------
    /**
     * The type of a token.
     */
    public enum Type {
        L_PAREN("("), R_PAREN(")"), COMMA(","),

        AND("&"), OR("|"), XOR("^"), NOT("!"),

        IDENT(null), STRING(null), NUMBER(null), END(null), INVALID("?");

        /**
         * Constructor.
         * 
         * @param repr the string representation of the token.
         */
        Type(String repr) {
            _repr = repr;
            _symbol = "<" + name().toLowerCase().replace('_', '-') + ">";
        }

        /**
         * Return the syntax symbol of this token type.
         * 
         * @return the syntax symbol of this token type.
         */
        @Override
        public String toString() {
            return _symbol;
        }

        /**
         * Return the string representation of the token.
         * 
         * NOTE: use name() for enum name.
         * 
         * @return the string representation of the token.
         */
        public String getRepr() {
            return _repr;
        }

        /**
         * Return the representation of this Token when expected as input.
         * 
         * This will be the string representation if it is fixed, or the symbol
         * for tokens whose text varies, e.g. strings.
         * 
         * @return the representation of this Token when expected as input.
         */
        public String asExpected() {
            return _repr != null ? "'" + _repr + "'" : _symbol;
        }

        /**
         * The syntax symbol of this token type.
         */
        protected String _symbol;

        /**
         * String representation of this token.
         */
        protected String _repr;
    } // inner enum Token.Type

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param type the type of the Token.
     * @param column the 1-based column number of the input where the Token text
     *        started.
     */
    public Token(Type type, int column) {
        this(type, type.getRepr(), column);
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param type the type of the Token.
     * @param text the text represented by the Token.
     * @param column the 1-based column number of the input where the Token text
     *        started.
     */
    public Token(Type type, String text, int column) {
        _type = type;
        _text = text;
        _column = column;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a String representation of this Token.
     * 
     * @return a String representation of this Token.
     */
    @Override
    public String toString() {
        return getText() == null ? getType().toString()
                                 : getType() + " '" + getText() + "'";
    }

    // ------------------------------------------------------------------------
    /**
     * Return the argument value corresponding to a STRING or NUMBER Token.
     * 
     * @return the argument value corresponding to a STRING or NUMBER Token.
     */
    public Object getValue() {
        switch (getType()) {
        case STRING:
            return _text.substring(1, _text.length() - 1);
        case NUMBER:
            return getDouble();
        default:
            return getText();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the type of this token.
     * 
     * @return the type of this token.
     */
    public Type getType() {
        return _type;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the text represented by this token.
     *
     * Note that for strings, that includes the surrounding double quotes.
     * 
     * @return the text represented by this token.
     */
    public String getText() {
        return _text;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the length of the Token text.
     * 
     * @return the length of the Token text.
     */
    public int getLength() {
        return _text != null ? _text.length() : 0;
    }

    // ------------------------------------------------------------------------
    /**
     * Parse this token as a double.
     * 
     * @return the double value of the token text.
     * @throws NumberFormatException if the token is not a number.
     */
    public double getDouble() {
        return Double.parseDouble(_text);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the 1-based column number of the input where the Token text
     * started.
     * 
     * @return the 1-based column number of the input where the Token text
     *         started.
     */
    public int getColumn() {
        return _column;
    }

    // ------------------------------------------------------------------------
    /**
     * The type of this token.
     */
    protected Type _type;

    /**
     * The text represented by this token.
     */
    protected String _text;

    /**
     * The 1-based column number of the input where the Token text started.
     */
    protected int _column;
} // class Token