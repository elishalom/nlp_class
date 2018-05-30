package tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author Reut Tsarfaty
 * @date 30 September 2010
 * 
 * CLASS: Node
 * 
 * Definition: the root of a local subtree inside a directed tree
 * Role: define the ID/LP relation to other nodes in the tree
 * Responsibility: 
 * - access its own local properties
 * - access its own structural properties
 * - navigate inside the dominated subtree
 * - access dominated span of terminals
 * - distinguish node types (root, internal, leaf)
 * - identify projection nodes and handle them
 * ---> flaten
 * ---> binarize
 * ---> HD processes
 * ---> etc.
 * 
 * TODO: 
 * - navigate inside subtrees
 * - remove node
 * - remove subtree
 * - remove and re-attache daughters
 * - flatten projections
 * 
 */

public class Node {

	// list of daughters
	private List<Node> m_lstDaughters = new ArrayList<Node>();
	
	// parent node
	private Node m_nodeParent = null;
	
	// marks a root node
	private boolean m_bRoot = false;
	
	// marks a node with a String identifier, 
	private String m_sIdentifier = null;
	


	/**
	 * C'tors 
	 * @param s
	 *
	 */
	public Node() {
		super();
	}
	
	public Node(String sID) {
		super();
		setIdentifier(sID);
	}

	public Object clone()
	{
		Node n = new Node(getIdentifier());
		
		n.setRoot(isRoot());
				
		for (int i = 0; i < getDaughters().size(); i++) {
			if (getDaughters().get(i) instanceof Terminal)
			{
				Terminal td = (Terminal)getDaughters().get(i);
				Terminal tdc =(Terminal)td.clone();
				n.addDaughter(tdc);
				tdc.setParent(n);
				
			}
			else
			{
				Node nd = (Node)getDaughters().get(i);
				Node ndc =(Node)nd.clone();
				n.addDaughter(ndc);
				ndc.setParent(n);
			}
		}
		return n;
	}
		
	/**
	 * Methods for Daughters Access
	 */
	
	// add daughter at the end of the daughter's list
	public boolean addDaughter(Node nDaughter) {
		nDaughter.setParent(this);
		return m_lstDaughters.add(nDaughter);
	}
	
	public Node getParent() {
		return m_nodeParent;
	}

	public void setParent(Node mNodeParent) {
		m_nodeParent = mNodeParent;
	}

	// remove daughter and dominated subtree
	public boolean removeDaughter(Node nDaughter) {
		return m_lstDaughters.remove(nDaughter);
	}
	 
	// get number of daughters
	protected int getNumberOfDaughters() {
		return m_lstDaughters.size();
	}
	
	// get daughters list (referenced) 
	public List<Node> getDaughters() {
		return m_lstDaughters;
	}
	
	// a leaf node is a node with an empty list of children
	public boolean isLeaf() {
		return m_lstDaughters.isEmpty();
	}
	
	// get root status of current node
	public boolean isRoot() {
		return m_bRoot;
	}
	
	// get the status of current node
	public boolean isInternal() {
		return ((!isRoot()) && (!isLeaf()));
	}
	
	// root setter (also possible via c'tor)
	public void setRoot(boolean bRoot) {
		m_bRoot = bRoot;
	}

	// identifier getter
	public String getIdentifier() {
		return m_sIdentifier;
	}

	// identifier setter	
	public void setIdentifier(String mSIdentifier) {
		m_sIdentifier = mSIdentifier;
	}
	
	// toString - node label
	public String toString() {
		return m_sIdentifier;
	}
	
	// toString - dominated subtree
	public String toStringSubtree() {
		StringBuffer sb = new StringBuffer();
		
		if (!this.isLeaf())
		{
			sb.append("(");
			sb.append(this.toString());
			sb.append(" ");
			
			boolean bFirst = true;
			for (Iterator<Node> iterator = 
				getDaughters().iterator(); 
				iterator.hasNext();) 
			{
				Node n = (Node) iterator.next();
				if (n.isLeaf())
				{ 	
					if (!bFirst)
					{
						sb.append(" ");
					}
					else
					{
						bFirst = false;
					}
					sb.append(n.toString());
				}
				else
				{
					if (!bFirst)
					{
						sb.append(" ");
					}
					else
					{
						bFirst = false;
					}
					sb.append(n.toStringSubtree());
				}
			}
			sb.append(")");
		}
		else
		{
			sb.append(this.toString());
		}
		return sb.toString();		
	}

	public  List<String> getYield() {
		ArrayList<String> lst = new ArrayList<String>();
		if (isLeaf())
			lst.add(getIdentifier());
		for (int i = 0; i < getDaughters().size(); i++) {
			lst.addAll(((Node)getDaughters().get(i)).getYield());
		}	
		return lst;
	}

	public List<Node> getNodes(List<Node> lst) 
	{
		lst.add(this);
		
		if (isLeaf())
		{
			return lst;
		}
		else
		{	
			for (int i = 0; i < getDaughters().size(); i++) 
			{
				Node n = (Node)getDaughters().get(i);
				lst.addAll(n.getNodes(new ArrayList<Node>()));
			}
			return lst;
		}
	}
	
	public List<Node> getInternalNodes(List<Node> lst) 
	{	
		if (isLeaf())
		{
			return lst;
		}
		else
		{
			lst.add(this);
			for (int i = 0; i < getDaughters().size(); i++) 
			{
				Node n = (Node)getDaughters().get(i);
				lst.addAll(n.getInternalNodes(new ArrayList<Node>()));
			}
			return lst;
		}
	}

	public boolean isPreTerminal() {
		if (isLeaf()) 
			return false;
		return	(getDaughters().get(0).isLeaf());
	}
	
	public boolean isNonTerminal() {
		if (isLeaf()) return false;
		if (isPreTerminal()) return false;
		return true;
	}
	
	public List<Terminal> getTerminals() 
	{
		List<Terminal> lst = new ArrayList<Terminal>();
		if (this instanceof Terminal)
		{
			lst.add((Terminal)this);
			return lst;
		}
		else
		{
			for (int i = 0; i < getDaughters().size(); i++) {	
				lst.addAll(((Node)getDaughters().get(i)).getTerminals());
			}
			return lst;
		}
	}
	
	public List<Node> getLeaves() 
	{
		List<Node> lst = new ArrayList<Node>();
		if (this.getDaughters().isEmpty())
		{
			lst.add(this);
			return lst;
		}
		else
		{
			for (int i = 0; i < getDaughters().size(); i++) {	
				lst.addAll(((Node)getDaughters().get(i)).getLeaves());
			}
			return lst;
		}
	}

	public String getLabel() 
	{
		return getIdentifier();
	}

	// OURS
	private void addDaughters(List<Node> daughters){
		for (Node node: daughters) {
			addDaughter(node);
		}
	}

	// SO OURS, INSANE
	private void removeDaughters(List<Node> daughters){
		for (Node node: daughters) {
			removeDaughter(node);
		}
	}

	// OURS !!!!
	public void binarize() {
		// binarize only non binary bodes
		if (this.getDaughters().size() > 2) {
			List<Node> oldDaughters = this.getDaughters();
			// they little sisters now
			List<Node> rightSisters = oldDaughters.subList(1, oldDaughters.size()).stream().collect(Collectors.toList());
			// remove old daughters
			this.removeDaughters(rightSisters);

			// add new node with right grand-daughters
			String newName = String.join("|", rightSisters.stream().map(n -> n.getIdentifier()).collect(Collectors.toList()));
			Node newRightDaughter = new Node(newName);
			newRightDaughter.addDaughters(rightSisters);
			addDaughter(newRightDaughter);
		}
		for (Node daugter : getDaughters()) {
			daugter.binarize();
		}
	}

	// TODO - documentation
    public void markovize_horizontally(int order) {
	    // Just in case the tree has not been binarized
        assert m_lstDaughters.size() <= 2;

        // Decoration is relevant only when there's a right child
        if (m_lstDaughters.size() == 2) {
            getDaughters().get(1).markovize_horizontally(order);
        } else if (isRoot() && m_lstDaughters.size() == 1) {
            getDaughters().get(0).markovize_horizontally(order);
        }

        // Avoid decorating leaves and the root node
        if (!isInternal()) {
            return;
        }

        // Get parents
        ArrayList<Node> parents = new ArrayList<>();
        Node current = this.getParent();
        int remainingOrder = order;
        while (remainingOrder > 0 && current != null && !current.isRoot()) {
            parents.add(0, current);
            current = current.getParent();
            remainingOrder--;
        }

        if (parents.size() == 0) {
            return;
        }

        // Get sisters identifier
        List<Node> bigSisters = parents.stream().map(p -> p.getDaughters().get(0)).collect(Collectors.toList());
        String sistersIdentifier = String.join("|", bigSisters.stream().map(s -> s.getIdentifier()).collect(Collectors.toList()));
        String newIdentifier = String.format("%s/%s", sistersIdentifier, getIdentifier());
        setIdentifier(newIdentifier);
    }

	public void markovize(int hOrder) {
		// implement horizontal markovization. order -1 means full binarization (q2.2) annotating all siblings

	}
}
