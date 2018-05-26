package decode;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.*;

import tree.Node;
import tree.Terminal;
import tree.Tree;

public class Decode {

	public static Set<Rule> m_setGrammarRules = null;
	public static Map<String, Set<Rule>> m_mapLexicalRules = null;
	private static HashMap<String, Integer> m_ntIndices = null; //maps every possible non-terminal to an index
	private static HashMap<Event,Event> m_mapRHS2LHS = null; // maps non-terminal rules by RHS as key and LHS as values
	
    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory 
     */
	public static Decode m_singDecoder = null;
	    
	public static Decode getInstance(Grammar g)
	{
		if (m_singDecoder == null)
		{
			m_singDecoder = new Decode();
			m_setGrammarRules = g.getSyntacticRules();
			m_mapLexicalRules = g.getLexicalEntries();

			// add non-terminal index table
			Set<String> m_N = g.getNonTerminalSymbols();
			int i = 0 ;
			for (String ntSymbol : m_N){ m_ntIndices.put(ntSymbol,i++);}

			// add right hand-side to left hand-side hash map
			for (Rule r : m_setGrammarRules){
				m_mapRHS2LHS.put(r.getRHS(),r.getLHS());
			}
		}
		return m_singDecoder;
	}

	public Tree decode(List<String> input) {

		// Done: Baseline Decoder
		//       Returns a flat tree with NN labels on all leaves 

		Tree t = new Tree(new Node("TOP"));
		Iterator<String> theInput = input.iterator();
		while (theInput.hasNext()) {
			String theWord = (String) theInput.next();
			Node preTerminal = new Node("NN");
			Terminal terminal = new Terminal(theWord);
			preTerminal.addDaughter(terminal);
			t.getRoot().addDaughter(preTerminal);
		}

		// TODO: CYK decoder
		// preprocess lexical rules - represent the input as <Terminal Symbol,probability> hashmap
		List<HashMap<String,Double>> taggedInput = lexicalizeInput(input);

		// build prob chart
		int numOfWordsInInput = input.size();
		int numOfSynCategories = m_ntIndices.size();
		double[][][] probChart = new double[numOfWordsInInput+1][numOfWordsInInput+1][numOfSynCategories];
		Arrays.fill(probChart,Double.POSITIVE_INFINITY);// TODO - maybe think of another solutions ...
		BackPointer[][][] bpChart = new BackPointer[numOfWordsInInput][numOfWordsInInput][numOfSynCategories];

		// initialize first row of the table (length_of_span = 1)
		for (int i=0 ; i<numOfWordsInInput ; i++){
			HashMap<String, Double> hm = taggedInput.get(i);
			for (String symbol : hm.keySet()){
				for (Rule r : m_setGrammarRules){
					if (r.isUnary()){
						// TODO - improve performance by indexing rhs
						if (r.getRHS().toString() == symbol){
							double minusLogProb = r.getMinusLogProb() + hm.get(symbol);
							int symbolIndex = m_ntIndices.get(r.getLHS().toString());
							double currentProb = probChart[1][i][symbolIndex];
							if (minusLogProb < currentProb){
								probChart[1][i][symbolIndex] = minusLogProb;
								// TODO - backtrack somehow
							}
						}
					}
				}
			}
		}

		//deal with binary rules
		for (int span = 2 ; span < numOfWordsInInput ; span++){
			for (int start = 0 ; start < span ; span ++){
				for (String ntSymbol : m_ntIndices.keySet()){
					double best = Double.POSITIVE_INFINITY;
					for (Rule r : m_setGrammarRules){
						// handling rules of form C -> C1 C2
						if (!r.isUnary()){
							BackPointer bp = null;
							int stop = start + span ;
							String C = r.getLHS().getSymbols().get(0);
							List<String> rRHS = r.getRHS().getSymbols();
							String C1 = rRHS.get(0);
							String C2 = rRHS.get(1);
							for (int mid = start + 1 ; mid < stop - 1 ; mid++) {
								double t1 = probChart[start][mid][m_ntIndices.get(C1)];
								double t2 = probChart[start][mid][m_ntIndices.get(C2)];
								double candidate = t1 + t2 + r.getMinusLogProb(); //addition instead of multiplication due to Log rules TODO - make sure there is no "infinity" overflow
								if (candidate < best ){
									best = candidate ;
									BackPointer bp1 = bpChart[start][mid][m_ntIndices.get(C1)];
									BackPointer bp2 = bpChart[start][mid][m_ntIndices.get(C2)];
									bp = new BackPointer(r,bp1,bp2);
								}
							}
							probChart[start][stop][m_ntIndices.get(C)] = best;
							bpChart[start][stop][m_ntIndices.get(C)] = bp;
						}
					}
				}
			}
		}


		//deal with unary rules


		//       if CYK fails,
		//       use the baseline outcome
		Tree cyk_t = buildTree(probChart,bpChart);
		if (cyk_t != null)
			t = cyk_t;

		return t;

	}

	private Tree buildTree(double[][][] probChart, BackPointer[][][] bpChart) {
	}

	/**
	 * pre-process method that returns POS tag probabilites for terminals in given input
	 * @param input - list of strings (
	 * @return posTagsProbs - list of hashmaps according to possible pos tags and their probability
	 */
	private List<HashMap<String,Double>> lexicalizeInput(List<String> input) {
		List<HashMap<String,Double>> posTagsProbs = new ArrayList<>();
		for (int i = 1; i < input.size() ; i++) {
			String word = input.get(i);
			HashMap<String, Double> hm = new HashMap<>();
			Set<Rule> lexRules = m_mapLexicalRules.get(word);
			// if the terminal did'nt appear in the training - tag it as NN using the default NN -> UNK rule
			if (lexRules == null) hm.put("NN", 0.0);
				// else - append appropriate terminal symbols and their according minusLogProbs
			else {
				for (Rule r : lexRules) {
					hm.put(r.getLHS().toString(), r.getMinusLogProb());
				}
			}
			posTagsProbs.add(hm);
		}
		return posTagsProbs;
	}

	/**
	 * Backpointer that holds indices for the probability chart
	 */
	private class BackPointer{
		Rule r;
		BackPointer bp1;
		BackPointer bp2;

		public BackPointer(Rule binRule, BackPointer bp1, BackPointer bp2){
			this.r = binRule;
			this.bp1 = bp1;
			this.bp2 = bp2;
		}

		public BackPointer(Rule unaryRule, BackPointer bp){
			this(unaryRule,bp,null);
		}

	}
}
