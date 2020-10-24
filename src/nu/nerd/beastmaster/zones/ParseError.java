package nu.nerd.beastmaster.zones;

// ----------------------------------------------------------------------------
/**
 * Represents a parse error.
 */
@SuppressWarnings("serial")
public class ParseError extends RuntimeException {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param message the base error message.
     * @param token the Token where the error occurred.
     */
    public ParseError(String message, Token token) {
        super(message);
        _token = token;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Token where the error occurred.
     * 
     * @return the Token where the error occurred.
     */
    public Token getToken() {
        return _token;
    }

    // ------------------------------------------------------------------------
    /**
     * The Token where the error occurred.
     */
    protected Token _token;
} // class ParseErro