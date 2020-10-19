package nu.nerd.beastmaster.zones.nodes;

import nu.nerd.beastmaster.zones.Expression;
import nu.nerd.beastmaster.zones.ExpressionVisitor;

// ----------------------------------------------------------------------------
/**
 * An {@link Expression} node representing an '^' operator.
 */
public class XorExpression extends Expression {
    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.Expression#visit(nu.nerd.beastmaster.zones.ExpressionVisitor,
     *      java.lang.Object)
     */
    @Override
    public Object visit(ExpressionVisitor visitor, Object context) {
        return visitor.visit(this, context);
    }
} // class XorExpression