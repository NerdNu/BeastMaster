package nu.nerd.beastmaster.zones.nodes;

import nu.nerd.beastmaster.zones.Expression;
import nu.nerd.beastmaster.zones.ExpressionVisitor;

// ----------------------------------------------------------------------------
/**
 * A Zone Specification sub-expression that represents a numeric argument to a
 * predicate.
 */
public class NumberExpression extends Expression {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param number the value of the number as a double.
     */
    public NumberExpression(double number) {
        _number = number;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the value of the number as a double.
     * 
     * @return the value of the number as a double.
     */
    public double getNumber() {
        return _number;
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
     * The value of the number as a double.
     */
    protected double _number;
} // class NumberExpression