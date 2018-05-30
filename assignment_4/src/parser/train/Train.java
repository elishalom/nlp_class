package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import tree.Node;
import tree.Tree;
import treebank.Treebank;
import utils.CyclicQueue;


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

	public void updateRuleProbs(Grammar grammar){
        HashMap<Rule, Integer> countRules = grammar.getRuleCounts();
		// TODO - fix occurences? (should be seperately counted by lhs of each rule (conditioned)
        double occurrences = (double)IntStream.of(countRules.values().stream().mapToInt(v -> v).toArray()).sum();
		for (Map.Entry<Rule, Integer> ruleCount : countRules.entrySet()) {
			ruleCount.getKey().setMinusLogProb(- Math.log(ruleCount.getValue()/occurrences));
		}
	}

	// Treebank Binarization by horizontal markovization parameter h
	public void binarizeTreeBank(Treebank treebank, int hOrder) {
		Queue<Node> sistersQueue;
		if (hOrder == -1) {
			sistersQueue = new LinkedList<Node>();
		} else {
			sistersQueue = new CyclicQueue<Node>(hOrder);
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
		if (numOfdaughters == 2){
			sistersQueue.clear();
			binarizeNode(leftChild,sistersQueue);
			rightChild = daughters.get(1);
			sistersQueue.add(leftChild); // remember left sister for annotation
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
		String sistersReresentation = sb.toString();
		rightChild.setIdentifier(cuddentId + "@/" + sistersReresentation);
		rightChild.setRoot(false);
		rightChild.setParent(currentNode);
	}


//	// recursively perofrom binarization (and markovization) for each node and it's children
//	private void binarizeTree(Tree t, int hOrder) {
//		binarizeNode(t.getRoot(), hOrder);
//	}
//
//	// recursive binarization + markovization of nodes
//	private void binarizeNode(Node node, int hOrder) {
//		// stop when reaching leaves
//		if (node == null || node.isLeaf()){
//			return;
//		}
//		List<Node> lst_daughters = node.getDaughters();
//		int numOfDaughters = lst_daughters.size();
//
//		// if unary - recuresivly binarize single child
//		if (numOfDaughters == 1){
//			binarizeNode(lst_daughters.get(0),hOrder);
//		}
//
//		// if binary - recuresivly binarize both children
//		else if (numOfDaughters == 2){
//			binarizeNode(lst_daughters.get(0),hOrder);
//			binarizeNode(lst_daughters.get(1),hOrder);
//		}
//
//		// if more than 2 children - re annotate in binary form by dividing the right sisters
//		List<Node> rightSisters = lst_daughters.subList(1,numOfDaughters);
//		annotateNode(node, rightSisters, hOrder);
//		// binaraize new children
//		binarizeNode(lst_daughters.get(0),hOrder);
//		binarizeNode(lst_daughters.get(1),hOrder);
//	}
//
//	// annotate nodes with more than 2 children according to markovization factor
//	private void annotateNode(Node newParent, List<Node> rightSisters, int hOrder) {
//		// dummy node will hold all the right sisters
//		Node dummyNode = new Node("~");
//		dummyNode.setParent(newParent);
//
//		// initialize name string with elder sisrer's name
//		String sistersNames = newParent.getDaughters().get(0).getIdentifier();
//		// disconnect the right sisters and add them as daughters to the dummy node
//		Iterator<Node> n_iter = rightSisters.iterator();
//		StringBuilder sb = new StringBuilder();
//		sb.append(sistersNames);
//		while (n_iter.hasNext()){
//			Node daughter = n_iter.next();
//			sb.append(" ").append(daughter.getIdentifier());
//			dummyNode.addDaughter(daughter);
//			daughter.setParent(dummyNode);
//			n_iter.remove();
//		}
//		sistersNames = sb.toString();
//
//		// connect dummy node to the new parent
//		newParent.addDaughter(dummyNode);
//		String parentName = newParent.getIdentifier();
//
//		// check if the parent is a binarized node (has fictive non-terminal)
//		if (parentName.contains("@")){
//			// split siblings by "|" charecter
//			String[] splitAnnotation = parentName.split(Pattern.quote("|"));
//			String rootSymbol = splitAnnotation[0];
//			// reorgenize sister names
//			String[] sisters;
//			// skip white space signs and anotate sisters division by it
//			if (splitAnnotation[1].charAt(0) == ' '){
//				sisters = splitAnnotation[1].substring(1).split(" "); }
//			else{ sisters = splitAnnotation[1].split(" "); }
//			// holds the number of sisters to remember by the markov factor
//			sb = new StringBuilder();
//			for (int i=0; i < hOrder && i < sisters.length ; i ++){
//				sb.append(" ").append(sisters[i]);
//			}
//			String symbolizedSisters = sb.toString();
//
//			for (int i = 1; i < sisters.length ; i++){
//				sb.append(" ").append(sisters[i]);
//			}
//			String alternateName = sb.toString();
//
//			// set dummy node name
//			String newName = rootSymbol + "|";
//			// if there sisters are to new symbol name - skip first space
//			if (symbolizedSisters.length() !=0){
//				newName += symbolizedSisters.substring(1);
//			}
//			newName += "|";
//			// delete garbage signs
//			dummyNode.setIdentifier(newName.replace("~ ","").replace("~",""));
//			// if there are more children
//			sb = new StringBuilder();
//
//			if (alternateName.length() == 0) {
//				return;
//			}
//			alternateName = rootSymbol + "|" + alternateName.substring(1) + "|";
//			dummyNode.setIdentifier(alternateName);
//		} // end treatment in fictive parent
//		// handle a real symbol parent
//		else{
//			String[] splitAnnotation = sistersNames.split(" ");
//			// represent redundant sisters by ~ sign
//			String garbageSigns = "";
//			// regarding 2 reserved names
//			for (int i=1 ; i <hOrder && i<splitAnnotation.length-3 ; i ++){
//				garbageSigns+= "~ ";
//			}
//
//			String sistersToRemember = "";
//			for (int i=0 ; i < splitAnnotation.length-2; i++){
//				sistersToRemember += " " + splitAnnotation[i];
//			}
//
//			// in case of infinite number of sisters ot remember (full binarization)
//			if (hOrder == -1){
//				for (int i =0 ; i < splitAnnotation.length - 3 ; i ++){
//					garbageSigns += "~ ";
//				}
//			}
//			sistersToRemember = sistersToRemember.substring(1);
//			dummyNode.setIdentifier("@"+parentName+"|"+garbageSigns+sistersToRemember);
//		}
//	} # TODO - maybe replace? a little faulty

}





