package nu.nerd.beastmaster.zones;

// ----------------------------------------------------------------------------
/**
 * Represents a parse error.
 */
@SuppressWarnings("serial")
public class ParseError extends RuntimeException {
    /**
     * Constructor.
     * 
     * @param message the base error message.
     * @param column the 1-based column where the error occurred.
     */
    public ParseError(String message, int column) {
        super(message);
        _column = column;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the 1-based column where the error occurred.
     * 
     * @return the 1-based column where the error occurred.
     */
    public int getColumn() {
        return _column;
    }

    // ------------------------------------------------------------------------
    /**
     * 1-based column where the error occurred.
     */
    protected int _column;
} // class ParseError