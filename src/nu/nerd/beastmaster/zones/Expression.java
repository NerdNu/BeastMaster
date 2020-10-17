package nu.nerd.beastmaster.zones;

import java.util.ArrayList;

// ----------------------------------------------------------------------------
/**
 * A node in the tree that represents a Zone Specification expression.
 * 
 * Nodes can have one or more children. Child nodes must be evaluated before
 * their parent.
 */
public abstract class Expression {
    // ------------------------------------------------------------------------
    /**
     * Add a child subexpression.
     * 
     * @param child the child.
     */
    public void addChild(Expression child) {
        _children.add(child);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the number of child nodes.
     * 
     * @return the number of child nodes.
     */
    public int getChildCount() {
        return _children.size();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the child at teh specified 0-based index.
     * 
     * @return the child at teh specified 0-based index.
     */
    public Expression getChild(int index) {
        return _children.get(index);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the first child node.
     * 
     * @return the first child node.
     */
    public Expression firstChild() {
        return getChild(0);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the second child node.
     * 
     * @return the second child node.
     */
    public Expression secondChild() {
        return getChild(1);
    }

    // ------------------------------------------------------------------------
    /**
     * Visit this node using the specified Visitor.
     * 
     * @param visitor defines what it means to "visit" this node. That includes
     *        the order in which children are visited and what actions are
     *        performed.
     */
    public abstract void visit(ExpressionVisitor visitor);

    // ------------------------------------------------------------------------
    /**
     * The children of this node.
     */
    protected ArrayList<Expression> _children = new ArrayList<>();
} // class Expression