package nu.nerd.beastmaster.zones.nodes;

import nu.nerd.beastmaster.zones.Expression;
import nu.nerd.beastmaster.zones.ExpressionVisitor;

// ----------------------------------------------------------------------------
/**
 * An {@link Expression} node representing an '|' operator.
 */
public class OrExpression extends Expression {
    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.Expression#visit(nu.nerd.beastmaster.zones.ExpressionVisitor)
     */
    @Override
    public void visit(ExpressionVisitor visitor) {
        visitor.visit(this);
    }
} // class OrExpression