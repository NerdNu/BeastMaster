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

        IDENT(null), STRING(null), NUMBER(null), END(null);

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
     */
    public Token(Type type) {
        this(type, type.getRepr());
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param type the type of the Token.
     * @param text the text represented by the Token.
     */
    public Token(Type type, String text) {
        _type = type;
        _text = text;
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
                                 : getType() + " \"" + getText() + "\"";
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
     * @return the text represented by this token.
     */
    public String getText() {
        return _text;
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
     * The type of this token.
     */
    protected Type _type;

    /**
     * The text represented by this token.
     */
    protected String _text;

} // class Token