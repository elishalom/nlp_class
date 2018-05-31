package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;
import tree.Node;
import tree.Tree;
import treebank.Treebank;
import utils.CircularFifoQueue;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 
 * @author Reut Tsarfaty
 * 
 * CLASS: Train
 * 
 * Definition: a learning component
 * Role: reads off a grammar from a treebank
 * Responsibility: keeps track of rule counts
 * 
 */

public class Train {


    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory 
     */
	public static Train m_singTrainer = null;
	    
	public static Train getInstance()
	{
		if (m_singTrainer == null)
		{
			m_singTrainer = new Train();
		}
		return m_singTrainer;
	}
	
	public static void main(String[] args) {

	}
	
	public Grammar train(Treebank myTreebank)
	{
		Grammar myGrammar = new Grammar();
		for (int i = 0; i < myTreebank.size(); i++) {
			Tree myTree = myTreebank.getAnalyses().get(i);
			List<Rule> theRules = getRules(myTree);
			myGrammar.addAll(theRules);

			// add start symbols to grammer TODO - make sure it's correct ... or if maybe I should leave it with "S" alone
			myGrammar.addStartSymbol(myTree.getRoot().getLabel());

			myTree.getRoot().getDaughters().forEach( d -> myGrammar.addStartSymbol(d.getIdentifier()));
		}

		updateRuleProbs(myGrammar);

		return myGrammar;
	}

	public List<Rule> getRules(Tree myTree)
	{
		List<Rule> theRules = new ArrayList<Rule>();

		List<Node> myNodes = myTree.getNodes();
		for (int j = 0; j < myNodes.size(); j++) {
			Node myNode = myNodes.get(j);
			if (myNode.isInternal())
			{
				Event eLHS = new Event(myNode.getIdentifier());
				Iterator<Node> theDaughters = myNode.getDaughters().iterator();
				StringBuffer sb = new StringBuffer();
				while (theDaughters.hasNext()) {
					Node n = (Node) theDaughters.next();
					sb.append(n.getIdentifier());
					if (theDaughters.hasNext())
						sb.append(" ");
				}
				Event eRHS = new Event (sb.toString());
				Rule theRule = new Rule(eLHS, eRHS);
				if (myNode.isPreTerminal())
					theRule.setLexical(true);
				if (myNode.isRoot())
					theRule.setTop(true);
				theRules.add(theRule);
			}
		}
		return theRules;
	}

	private void updateRuleProbs(Grammar grammar) {
		HashMap<Rule, Integer> ruleCounts = grammar.getRuleCounts();
		for (Rule r : ruleCounts.keySet()) {
			double lhsCount;
			// if lexical rule
			if (r.isLexical())
				lhsCount = (double) grammar.getLexLHSSymbolCounts().get(r.getLHS());
			// if syntactic rule
			else
				lhsCount = (double) grammar.getSynLHSSymbolCounts().get(r.getLHS());
			double minLogProb = ((1.0 * ruleCounts.get(r)) / lhsCount);
			r.setMinusLogProb(-Math.log(minLogProb));
		}
	}

//		old update probs code code!!! return only if problematic
// 		HashMap<Rule, Integer> countRules = grammar.getRuleCounts();
//		TODO - fix occurences? (should be seperately counted by lhs of each rule (conditioned)
//		double occurrences = (double)IntStream.of(countRules.values().stream().mapToInt(v -> v).toArray()).sum();
//		for (Map.Entry<Rule, Integer> ruleCount : countRules.entrySet()) {
//			ruleCount.getKey().setMinusLogProb(- Math.log(ruleCount.getValue()/occurrences));
//		}
//	}

	// Treebank Binarization by horizontal markovization parameter h
	public void binarizeTreeBank(Treebank treebank, int hOrder) {
		Queue<Node> sistersQueue;
		if (hOrder == -1) {
			sistersQueue = new LinkedList<Node>();
		} else {
			sistersQueue = new CircularFifoQueue<Node>(hOrder);
		}
		treebank.getAnalyses().forEach(
				tree -> binarizeNode(tree.getRoot(), sistersQueue));
	}

	// recursively binarize all nodes and it's daughters according to markovization parameter
	private static void binarizeNode(Node currentNode, Queue<Node> sistersQueue) {
		// stop condition - reached leaf
		if (currentNode.isLeaf())
			return;
		List<Node> daughters = currentNode.getDaughters();
		Node leftChild = daughters.get(0);
		Node rightChild;
		int numOfdaughters = daughters.size();
		// if one child - binarize it and dismiss redundant sisters to remember
		if (numOfdaughters == 1){
			sistersQueue.clear();
			binarizeNode(leftChild,sistersQueue);
		}
		// if two children - clear redundant sisters to remember and binarize both
		else if (numOfdaughters == 2) {
			sistersQueue.clear();
			binarizeNode(leftChild, sistersQueue);
			rightChild = daughters.get(1);
			sistersQueue.add(leftChild); // remember left sister for annotation
			binarizeNode(rightChild, sistersQueue);
		}
		// else there are more than 2 children -> annotate and create fictive node
		else {
			rightChild = (Node) currentNode.clone();
			sistersQueue.add(leftChild);
			annotateNode(currentNode,rightChild,sistersQueue); // annotate binarized right child according to Markov factor
			leftChild = rightChild.getDaughters().remove(0); // update left child
			// disconnect current daughters and connect new annotated daughters
			daughters.clear();
			currentNode.addDaughter(leftChild);
			currentNode.addDaughter(rightChild);
			// binarize grandchildren recursively
			binarizeNode(rightChild,sistersQueue);
			sistersQueue.clear();
			binarizeNode(leftChild, sistersQueue);
		}
	}

	// annotate the new child using representation of the sisters to remember
	private static void annotateNode(Node currentNode, Node rightChild, Queue<Node> sistersQueue) {
		String cuddentId = currentNode.getIdentifier().split("\\p{Punct}")[0];
		StringBuilder sb = new StringBuilder();
		for (Node sister : new ArrayList<>(sistersQueue)){
			sb.append(sister).append("/");
		}
		String sistersRepresentation = sb.toString();
		rightChild.setIdentifier(cuddentId + "@/" + sistersRepresentation);
		rightChild.setRoot(false);
		rightChild.setParent(currentNode);
	}

	// de-binarization by adding children of fictive nodes to their parents and de-annotate
	public void debinarizeTreeList(List<Tree> parseTrees) {
		parseTrees.forEach(tree -> deBinarizeNode(tree.getRoot()));
	}

	// recuresively de-binarize and de-annotate nodes and its children
	private void deBinarizeNode(Node currentNode) {
		// stop condition - do not operate on leaves
		if (currentNode.isLeaf()){
			return;
		}
		List<Node> daughters = currentNode.getDaughters();
		// current node is at least unary. binarization left always non-fictive lef-child
		Node leftDaughter = daughters.get(0);
		// recursively de-binarize the node's left daughter
		deBinarizeNode(leftDaughter);
		// if unary, no other daughter to debinarize
		if (daughters.size() == 1){
			return;
		}
		// otherwise the node is binary. recursively debinarize it's right daughter
		Node rightDaughter = daughters.get(1);
		deBinarizeNode(rightDaughter);

		// if current node is fictive - disconnect the node and add it's children to it's parent
		if (currentNode.getIdentifier().contains("@")){
			Node currentParent = currentNode.getParent();
			currentParent.removeDaughter(currentNode);
			daughters.forEach(currentParent::addDaughter);
		}
	}
}





