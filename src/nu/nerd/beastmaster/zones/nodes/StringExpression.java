package nu.nerd.beastmaster.zones.nodes;

import nu.nerd.beastmaster.zones.Expression;
import nu.nerd.beastmaster.zones.ExpressionVisitor;

// ----------------------------------------------------------------------------
/**
 * A Zone Specification sub-expression that represents a string argument to a
 * predicate.
 */
public class StringExpression extends Expression {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param text the text of the string (between the quotes).
     */
    public StringExpression(String text) {
        _text = text;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the text of the string (between the quotes).
     * 
     * @return the text of the string (between the quotes).
     */
    public String getText() {
        return _text;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.Expression#visit(nu.nerd.beastmaster.zones.ExpressionVisitor,
     *      java.lang.Object)
     */
    @Override
    public Object visit(ExpressionVisitor visitor, Object context) {
        return visitor.visit(this, context);
    }

    // ------------------------------------------------------------------------
    /**
     * The text of the string (between the quotes).
     */
    protected String _text;
} // class StringExpression