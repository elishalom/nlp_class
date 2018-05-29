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
	private static HashMap<String,HashMap<String, Double>> m_RHSindexedUnaryGrammar;
	private static HashMap<String,HashMap<String, Double>> m_RHSindexedBinaryGrammar;

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

			// index grammar by RHS and split to unary and binary rules
			Set<Rule> unaryRules = new HashSet<>();
			Set<Rule> binaryRules = new HashSet<>();
			splitRules(m_setGrammarRules,unaryRules,binaryRules);

			m_RHSindexedUnaryGrammar = indexGrammarByRHS(unaryRules);
			m_RHSindexedBinaryGrammar = indexGrammarByRHS(binaryRules);
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
		// set probChart as 3d array that holds non-terminal,minusLogProb as values
		ArrayList<ArrayList<HashMap<String,Double>>> probChart = createPyramidTable(input.size());
		// set backpointers chart for later backtrace and build tree
		ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart = createPyramidTable(input.size());
		// build probability and backpointer charts according to CYK algorithm
		buildCYKCharts(probChart,bpChart,input);

		ArrayList<BackPointer> possibleBackPointers = getBackPointers(bpChart);
		BackPointer minBackPointer = getBestParse(possibleBackPointers);
		Tree bestParse = buildTree(minBackPointer);

		// if CYK fails, use the baseline outcome
		if (bestParse != null)
			t = bestParse;

		return t;
	}

	/**
	 * creates a 'steps' charts as used in CKY (pyramid-like 3-d array)
	 * @param size
	 * @return empty pyramid table with base length of given input
	 */
	private <S,T> ArrayList<ArrayList<HashMap<S,T>>> createPyramidTable(int size) {
		ArrayList<ArrayList<HashMap<S,T>>> pyramidTable = new ArrayList<>();
		for (int row = 0 ; row < size ; row++){
			pyramidTable.add(new ArrayList<>());
			for (int col = 0 ; col < row + 1 ; col ++){
				pyramidTable.get(row).add(new HashMap<S,T>());
			}
		}
		return  pyramidTable;
	}

	// TODO - documentation
	// build probability backpointers charts for CKY
	private void buildCYKCharts(ArrayList<ArrayList<HashMap<String, Double>>> probChart, ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart, List<String> input) {
		// handle Terminal rules to initialize 1st row (lexical preprocess)
		parseTerminals(probChart,bpChart, input);
		// handle syntactic rules
		parseSynRules(probChart,bpChart);
		return;
	}

	// TODO - documentation - parsing terminals and building the CYK Charts
	private void parseTerminals(ArrayList<ArrayList<HashMap<String,Double>>> probChart, ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart, List<String> input) {
		int numOfWordsInInput = input.size();
		int lastRowIndex = numOfWordsInInput - 1 ;
		List<HashMap<String, Double>> lexedInput = lexicalizeInput(input); // represent the input as list of possible tag-probability hashmaps per word
		for (int i = 0; i < numOfWordsInInput ; i++){
			probChart.get(lastRowIndex).get(i).putAll(lexedInput.get(i)); // add all symbol-minLogProbs to probchart
			for (String terminalSymbol : lexedInput.get(i).keySet()){
				BackPointer bp = new BackPointer(new ChartIndex(-1,-1,input.get(i))); // all terminals point to non-existant indices yet hold the original word value
				bpChart.get(lastRowIndex).get(i).put(terminalSymbol,bp);
			}

			// handle unary rules that could derive the terminal symbol
			boolean stop = false;
			while(!stop){
				stop = true; // by default stop the loop (continue only if there was an improvement. not infinite since we add probs and check for minimum
				// check all unary rules
				for (String rhs : m_RHSindexedUnaryGrammar.keySet()){
					Double terminalLogProb = probChart.get(lastRowIndex).get(i).get(rhs);
					// if the inserted symbol could be derived from an unary rule
					if (terminalLogProb != null){
						// for all the unary rules lhs -> rhs
						for (String lhs : m_RHSindexedUnaryGrammar.get(rhs).keySet()){
							Double unaryLogProb = probChart.get(lastRowIndex).get(i).get(lhs);
							// if never seen this rule
							if (unaryLogProb == null){
								// continue to loop
								stop = false;
								// update probaility in the probChart - adding minus log probabilities
								probChart.get(lastRowIndex).get(i).put(lhs,terminalLogProb + m_RHSindexedUnaryGrammar.get(rhs).get(lhs));
								// set according bpChart to the previous index (point to the last row, according word index and the symbol value)
								BackPointer bp = new BackPointer(new ChartIndex(lastRowIndex,i,rhs));
								bpChart.get(lastRowIndex).get(i).put(lhs,bp);
							}
							// else - we have already derived the symbol! we should check if we achieved improvement
							else{
								// compare probs
								double newProb = terminalLogProb + m_RHSindexedUnaryGrammar.get(rhs).get(lhs);
								// if it somehow improves continue loop
								if (newProb < unaryLogProb){
									// continue the loop
									stop = false;
									// update probChart
									probChart.get(lastRowIndex).get(i).put(lhs,newProb);
									// update backpointers entry
									BackPointer newBP = new BackPointer(new ChartIndex(lastRowIndex,i,rhs));
									bpChart.get(lastRowIndex).get(i).put(lhs,newBP);
								}
							}
						}
					}
				}
			}// end parse unary rules
		}// end iterating over input
	}

	// TODO - documentation - filling the charts according to CYK algorithm
	private void parseSynRules(ArrayList<ArrayList<HashMap<String,Double>>> probChart, ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart) {
		int numOfRows = probChart.size();
		// iterate over the table starting with row (size+1-span) 2 until reaching the top
		for (int span = 2 ; span <= numOfRows ; span ++){
			int row = numOfRows - span;
			// iterate over possible splits for the pharses
			int numOfCols = probChart.get(row).size();
			for (int start = 0; start < numOfCols ; start ++){ //start of span index
				// iterate over different splits of the span
				for (int split = 0 ; split < span - 1 ; split ++){ //TODO - check the index issues!!
					// handle binary rules
					// for all the binary rules find if possible production of rule lhs -> rhs1 rhs2
					// iterate by right hand-side
					for (String rhs : m_RHSindexedBinaryGrammar.keySet()){
						String rhs1 = rhs.split(" ")[0];
						String rhs2 = rhs.split(" ")[1];
						// check if the relevant cells hold the relevant productions
						ChartIndex firstChildIndex = new ChartIndex(row + span - 1 - split,start,rhs1);
						ChartIndex secondChildIndex = new ChartIndex(row + 1 + split,start + split + 1,rhs2);

						Double rhs1Prob = probChart.get(firstChildIndex.row).get(firstChildIndex.col).get(firstChildIndex.sym);
						Double rhs2Prob = probChart.get(secondChildIndex.row).get(secondChildIndex.col).get(secondChildIndex.sym);
						// if the relevant children exist
						if (rhs1Prob != null && rhs2Prob != null){
							// iterate over all possible parents (left side)
							for (String lhs : m_RHSindexedBinaryGrammar.get(rhs).keySet()){
								double ruleProb = m_RHSindexedBinaryGrammar.get(rhs).get(lhs);
								double splitProb = rhs1Prob + rhs2Prob + ruleProb; // sum instead of product (log probabilities)
								// update relevant entries in probChart and bpChart
								ChartIndex parentIndex = new ChartIndex(row,start,lhs);
								// update probchart
								probChart.get(parentIndex.row).get(parentIndex.col).put(parentIndex.sym,splitProb);
								// update bpChart
								BackPointer bp = new BackPointer(firstChildIndex,secondChildIndex);
								bpChart.get(parentIndex.row).get(parentIndex.col).put(parentIndex.sym,bp);
							}
						}
					}
				}

				//handle unary rules



			} // end iterating over columns
		} // end iterating over rows
		return;
	}


	private Tree buildTree(BackPointer bp) {
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
	private class BackPointer {
		ChartIndex child1;
		ChartIndex child2;

		// invoked for Unary Rules
		public BackPointer(ChartIndex child){
			this(child,null);
		}

		// invoked for binary rules
		public BackPointer(ChartIndex child1, ChartIndex child2){
			this.child1 = child1;
			this.child2 = child2;
		}

		//TODO - notice special case for terminals - chartindex should be (-1,-1,word)
	}

	private void addUnaryRules(){

	}


	// TODO - doument
	// this method splits Set of Rules ruleset to binary and unary rules
	private static void splitRules(Set<Rule> rules,
										Set<Rule> unaryRules,
										Set<Rule> binaryRules) {
		for (Rule r : rules){
			if (r.isUnary())
				unaryRules.add(r);
			else
				binaryRules.add(r);
		}
	}

	// TODO - document - method gives a RHS indexed version of given grammar
	// notice - can check wheter a rule is unary or binary by the length of the rhs (split by " ")
	private static HashMap<String,HashMap<String,Double>> indexGrammarByRHS(Set<Rule> rules) {
		HashMap<String,HashMap<String,Double>> rhsIndexedGrammar = new HashMap<>();

		for (Rule r : rules){
			String lhs = r.getLHS().toString();
			String rhs = r.getRHS().toString();
			rhsIndexedGrammar.computeIfAbsent(rhs, k -> new HashMap<String, Double>());
			rhsIndexedGrammar.get(rhs).put(lhs,r.getMinusLogProb());
		}
		return rhsIndexedGrammar;
	}

	// generic tuple interface - used in indexGrammarByRHS to index
	private static class Tuple<X, Y> {
		X x;
		Y y;
		public Tuple(X x, Y y){
			this.x = x;
			this.y = y;
		}
		@Override
		public String toString() { return x + "<-" + y ;}
	}

	// chart index represented by row-column-symbol - used in BackPointer
	private class ChartIndex{
		int row;
		int col;
		String sym;

		ChartIndex(int row,int col,String sym){
			this.row = row;
			this.col = col;
			this.sym = sym;
		}
	}
}
