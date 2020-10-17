package nu.nerd.beastmaster.zones;

import nu.nerd.beastmaster.zones.nodes.AndExpression;
import nu.nerd.beastmaster.zones.nodes.NotExpression;
import nu.nerd.beastmaster.zones.nodes.NumberExpression;
import nu.nerd.beastmaster.zones.nodes.OrExpression;
import nu.nerd.beastmaster.zones.nodes.PredicateExpression;
import nu.nerd.beastmaster.zones.nodes.StringExpression;
import nu.nerd.beastmaster.zones.nodes.XorExpression;

// ----------------------------------------------------------------------------
/**
 * In order to handle traversal of the Expression tree representing a Zone
 * Specification, visitors must implement this interface.
 * 
 * This is an instance of the Visitor design pattern.
 */
public interface ExpressionVisitor {
    /**
     * Visit and {@link AndExpression} node.
     * 
     * @param node the {@link Expression} node.
     */
    public void visit(AndExpression node);

    /**
     * Visit and {@link OrExpression} node.
     * 
     * @param node the {@link Expression} node.
     */
    public void visit(OrExpression node);

    /**
     * Visit and {@link XorExpression} node.
     * 
     * @param node the {@link Expression} node.
     */
    public void visit(XorExpression node);

    /**
     * Visit and {@link NotExpression} node.
     * 
     * @param node the {@link Expression} node.
     */
    public void visit(NotExpression node);

    /**
     * Visit and {@link PredicateExpression} node.
     * 
     * @param node the {@link Expression} node.
     */
    public void visit(PredicateExpression node);

    /**
     * Visit and {@link NumberExpression} node.
     * 
     * @param node the {@link Expression} node.
     */
    public void visit(NumberExpression node);

    /**
     * Visit and {@link StringExpression} node.
     * 
     * @param node the {@link Expression} node.
     */
    public void visit(StringExpression node);
} // class ExpressionVisitor