package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.*;

import tree.Node;
import tree.Tree;
import treebank.Treebank;
import utils.CountMap;


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
		}

		// ugly - us - debugz
        HashMap<Rule, Double> est = estimateRuleProbs(myGrammar);
        for (Map.Entry<Rule, Double> entry : est.entrySet()){
            Rule rule = entry.getKey();
            Double prob = (double) entry.getValue();
            System.out.printf("%s\t%.2f\n",rule.toString(),prob);
        }
		// end debugz
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

	public HashMap<Rule, Double> estimateRuleProbs(Grammar grammar){
        HashMap<Rule, Double> estimateRuleProbs = new HashMap<>();
        HashMap<Rule, Integer> countRules = grammar.getRuleCounts();

        int numOfRule = 0;
        for (Map.Entry<Rule, Integer> entry : countRules.entrySet()){
            Rule rule = entry.getKey();
            Double prob = (double) entry.getValue();
            numOfRule ++;
            estimateRuleProbs.put(rule, prob);
        }

        for (Map.Entry<Rule, Double> entry : estimateRuleProbs.entrySet()){
            Rule rule = entry.getKey();
            Double prob = entry.getValue();
            estimateRuleProbs.put(rule, - Math.log(prob/numOfRule));
        }

        return  estimateRuleProbs;
	}

}
