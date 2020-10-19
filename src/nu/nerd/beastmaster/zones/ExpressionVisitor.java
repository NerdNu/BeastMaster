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
 * 
 * Visitation of the nodes in the Expression tree typically requires creation of
 * a result object.
 * 
 * The result can be:
 * <ol>
 * <li>Accrued in the ExpressionVisitor instance itself.</li>
 * <li>Accrued in the context parameter passed to the visit() method.</li>
 * <li>Returned by the visit() method.</li>
 * </ol>
 * 
 * It is not mandatory to provide a non-null context parameter or return a
 * non-null result.
 */
public interface ExpressionVisitor {
    /**
     * Visit and {@link AndExpression} node.
     * 
     * @param node the {@link Expression} node.
     * @param context context information representing the state of the
     *        traversal prior to visiting the node.
     * @return an object representing the result of visiting the node.
     */
    public Object visit(AndExpression node, Object context);

    /**
     * Visit and {@link OrExpression} node.
     * 
     * @param node the {@link Expression} node.
     * @param context context information representing the state of the
     *        traversal prior to visiting the node.
     * @return an object representing the result of visiting the node.
     */
    public Object visit(OrExpression node, Object context);

    /**
     * Visit and {@link XorExpression} node.
     * 
     * @param node the {@link Expression} node.
     * @param context context information representing the state of the
     *        traversal prior to visiting the node.
     * @return an object representing the result of visiting the node.
     */
    public Object visit(XorExpression node, Object context);

    /**
     * Visit and {@link NotExpression} node.
     * 
     * @param node the {@link Expression} node.
     * @param context context information representing the state of the
     *        traversal prior to visiting the node.
     * @return an object representing the result of visiting the node.
     */
    public Object visit(NotExpression node, Object context);

    /**
     * Visit and {@link PredicateExpression} node.
     * 
     * @param node the {@link Expression} node.
     * @param context context information representing the state of the
     *        traversal prior to visiting the node.
     * @return an object representing the result of visiting the node.
     */
    public Object visit(PredicateExpression node, Object context);

    /**
     * Visit and {@link NumberExpression} node.
     * 
     * @param node the {@link Expression} node.
     * @param context context information representing the state of the
     *        traversal prior to visiting the node.
     * @return an object representing the result of visiting the node.
     */
    public Object visit(NumberExpression node, Object context);

    /**
     * Visit and {@link StringExpression} node.
     * 
     * @param node the {@link Expression} node.
     * @param context context information representing the state of the
     *        traversal prior to visiting the node.
     * @return an object representing the result of visiting the node.
     */
    public Object visit(StringExpression node, Object context);
} // class ExpressionVisitor