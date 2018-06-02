package decode;

import com.sun.xml.internal.bind.v2.TODO;
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
	private static Set<String> m_setStartSymbols;
	private static HashMap<String, Double> m_startSymbolProb;

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

			//add start-symbols
			m_setStartSymbols = g.getStartSymbols();

			// add non-terminal index table
			m_ntIndices = new HashMap<>();
			Set<String> m_N = g.getNonTerminalSymbols();
			int i = 0 ;
			for (String ntSymbol : m_N){m_ntIndices.put(ntSymbol,i++);}

			// add right hand-side to left hand-side hash map
			m_mapRHS2LHS = new HashMap<>();
			for (Rule r : m_setGrammarRules){
				m_mapRHS2LHS.put(r.getRHS(),r.getLHS());
			}

			// index grammar by RHS and split to unary and binary rules
			Set<Rule> unaryRules = new HashSet<>();
			Set<Rule> binaryRules = new HashSet<>();
			splitRules(m_setGrammarRules,unaryRules,binaryRules);

			m_RHSindexedUnaryGrammar = indexGrammarByRHS(unaryRules);
			m_RHSindexedBinaryGrammar = indexGrammarByRHS(binaryRules);

			m_startSymbolProb = g.getStartSymbolsProb();
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

		// set probChart as 3d array that holds non-terminal,minusLogProb as values
		ArrayList<ArrayList<HashMap<String,Double>>> probChart = createPyramidTable(input.size());

		// set backpointers chart for later backtrace and build tree
		ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart = createPyramidTable(input.size());

		// build probability and backpointer charts according to CYK algorithm
		buildCYKCharts(probChart,bpChart,input);

		// get the minimal -logProb back pointer's index (0,0,symbol)
        Set<String> candidates = m_setStartSymbols;
		String bestBPSymbol = getMinimumBackPointerIndex(candidates,probChart);

		// build the tree using the backpointers
		// if CYK fails, use the baseline outcome
		if (bestBPSymbol != null){
			t = buildTree(bpChart,bestBPSymbol);
		}

		return t;
	}

	// building parse tree using the back pointer chart
	private Tree buildTree(ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart, String bpSymbol) {
		// create a root node
		Node root = new Node("TOP");
		root.setRoot(true);
		Tree t = new Tree(root);

        // set the minimal backPointer's symbol as sub-root
        Node firstNonTerminal = new Node(bpSymbol);
        root.addDaughter(firstNonTerminal);
        firstNonTerminal.setParent(root);

		// recover 1st backpointer
		BackPointer nextBP = bpChart.get(0).get(0).get(bpSymbol);

		// recursively add children to the nodes using the back pointers
		traceBack(firstNonTerminal, nextBP, bpChart);

		return t;
	}

	// recuresivley add children to nodes by the backpointer chart
	private void traceBack(Node parent, BackPointer currentBP, ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart){

		// if no children - it's a leaf
		if (currentBP.child1.col == -1){
		    // add the terminal node to the tree and return
            String word = currentBP.child1.sym;
            Terminal treminalNode = new Terminal(word);
            parent.addDaughter(treminalNode);
            treminalNode.setParent(parent);
			return;
		}

		// if unary add only one child
		if (currentBP.child2 == null){
			// add the single node to parent
			String label = currentBP.child1.sym;
			Node child = new Node(label);
			parent.addDaughter(child);
			child.setParent(parent);
			// recursively traceback on the child
			BackPointer nextBP = bpChart.get(currentBP.child1.row).get(currentBP.child1.col).get(currentBP.child1.sym);
			traceBack(child,nextBP,bpChart);
		}

		// if binary add 2 children
		else{
			// set children labels
			String leftLabel = currentBP.child1.sym;
			String rightLabel = currentBP.child2.sym;
			// create children nodes
			Node leftChild = new Node(leftLabel);
			Node rightChild = new Node(rightLabel);
			// append children to parent
			parent.addDaughter(leftChild);
			leftChild.setParent(parent);
			parent.addDaughter(rightChild);
			rightChild.setParent(parent);
			// traceback recursively over children (left to right)
			BackPointer nextLeftBP = bpChart.get(currentBP.child1.row).get(currentBP.child1.col).get(currentBP.child1.sym);
			traceBack(leftChild,nextLeftBP,bpChart);
			BackPointer nextRightChild = bpChart.get(currentBP.child2.row).get(currentBP.child2.col).get(currentBP.child2.sym);
			traceBack(rightChild,nextRightChild,bpChart);
		}
	}

	//iterate over start symbol candidates and returns the relevant symbol in the prob table that hold minimal value
	private String getMinimumBackPointerIndex(Set<String> candidates, ArrayList<ArrayList<HashMap<String,Double>>> probChart) {
		double best = Double.POSITIVE_INFINITY;
		String minSymbol = null;
		for (String symbol : candidates){
			Double drivationProb = probChart.get(0).get(0).get(symbol);
			if (drivationProb != null){
				double prob = drivationProb + m_startSymbolProb.get(symbol);
				if (prob < best){
					best = prob;
					minSymbol = symbol;
				}
			}
		}
		return minSymbol;
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

	// build probability backpointers charts for CKY
	private void buildCYKCharts(ArrayList<ArrayList<HashMap<String, Double>>> probChart, ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart, List<String> input) {
		// handle Terminal rules to initialize 1st row (lexical preprocess)
		parseTerminals(probChart,bpChart, input);
		// handle syntactic rules
		parseSynRules(probChart,bpChart);
		return;
	}

	// parsing terminals and building the CYK Charts
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
						    // probability of the rule in the grammar
                            double ruleProb = m_RHSindexedUnaryGrammar.get(rhs).get(lhs);
                            // new probability is product of the rule probability and the previous step
                            double newProb = terminalLogProb + ruleProb;
                            // check if we already seen the non-terminal lhs and get the calculated probability for it
							Double unaryLogProb = probChart.get(lastRowIndex).get(i).get(lhs);
							// if never seen this rule
							if (unaryLogProb == null){
								// continue to loop
								stop = false;
								// update probaility in the probChart - adding minus log probabilities
								probChart.get(lastRowIndex).get(i).put(lhs,newProb);
								// set according bpChart to the previous index (point to the last row, according word index and the symbol value)
								BackPointer bp = new BackPointer(new ChartIndex(lastRowIndex,i,rhs));
								bpChart.get(lastRowIndex).get(i).put(lhs,bp);
							}
							// else - we have already derived the symbol! we should check if we achieved improvement
							else{
								// if it somehow improves continue loop
								if (newProb < unaryLogProb){
									// continue the loop
									stop = false;
									// update probChart
									probChart.get(lastRowIndex).get(i).put(lhs,newProb);
									// update backpointers entry
									BackPointer newBP = new BackPointer(new ChartIndex(lastRowIndex,i,rhs));
									bpChart.get(lastRowIndex).get(i).remove(lhs);
									bpChart.get(lastRowIndex).get(i).put(lhs,newBP);
								}
							}
						}
					}
				}
			}// end parse unary rules
		}// end iterating over input
	}

	// filling the charts according to CYK algorithm (for syntactic rules)
	private void parseSynRules(ArrayList<ArrayList<HashMap<String,Double>>> probChart, ArrayList<ArrayList<HashMap<String,BackPointer>>> bpChart) {
		int numOfRows = probChart.size();
		// iterate over the table starting with row (size+1-span) 2 until reaching the top
		for (int span = 2 ; span <= numOfRows ; span ++){
			int row = numOfRows - span;
			// iterate over possible splits for the pharses
			int numOfCols = probChart.get(row).size();
			for (int start = 0; start < numOfCols ; start ++){ //start of span index
				// iterate over different splits of the span
				for (int split = 0 ; split < span - 1 ; split ++){
					// check if the relevant cells hold the relevant productions
					int child1Row = row + span - 1 - split;
					int child1Col = start;
					Set<String> possibleRHS1Symbols = probChart.get(child1Row).get(child1Col).keySet();

					int child2Row = row + 1 + split;
					int child2Col = start + split + 1;
					if (row == 18 && start == 18){
						int x = 0 ;
					}
					Set<String> possibleRHS2Symbols = probChart.get(child2Row).get(child2Col).keySet();

					// represent all possible right side combinations from our CKY table (from symbols in relevant cells)
					HashSet<String> possibleRHSs = new HashSet<>();
					for (String sym1 : possibleRHS1Symbols){
						for (String sym2: possibleRHS2Symbols) {
							possibleRHSs.add(sym1+" "+sym2);
						}
					}

					for (String rhs : possibleRHSs){
						// check if there are relevent rules for the possible RHS combinations
						HashMap<String,Double> possibleLHSs = m_RHSindexedBinaryGrammar.get(rhs);
						// if there are rules in the grammar such as lhs -> rhs1 rhs2
						if (possibleLHSs != null){
							// create relevant indices for bpChart and calculate relevant probs from the probChart
							// handle first element of the RHS
							String rhs1 = rhs.split(" ")[0];
							ChartIndex child1Index = new ChartIndex(child1Row,child1Col,rhs1);
							double rhs1Prob = probChart.get(child1Index.row).get(child1Index.col).get(rhs1);
							// handle 2nd element of RHS
							String rhs2 = rhs.split(" ")[1];
							ChartIndex child2Index = new ChartIndex(child2Row,child2Col,rhs2);
							double rhs2Prob = probChart.get(child2Index.row).get(child2Index.col).get(rhs2);
							// for each possible lhs
							for (String lhs : possibleLHSs.keySet()){
								// get the rule probability from the  grammar
								double ruleProb = m_RHSindexedBinaryGrammar.get(rhs).get(lhs);
								// calculate total probability of the rules (multiplication as sum of logs)
								double newProb = ruleProb + rhs1Prob + rhs2Prob;
                                Double oldProb = probChart.get(row).get(start).get(lhs);
								// update probChart and bpChart if improve previous prob
                                if (oldProb == null || newProb < oldProb){
                                    ChartIndex parentIndex = new ChartIndex(row,start,lhs);
                                    // update prob
                                    probChart.get(parentIndex.row).get(parentIndex.col).put(lhs,newProb);
                                    // update bpChart
                                    BackPointer bp = new BackPointer(child1Index,child2Index);
                                    bpChart.get(parentIndex.row).get(parentIndex.col).put(parentIndex.sym,bp);
                                }
							}
						} // end iterating over possible LHS of rules
					} // end iterating over possible RHS of potential rules
				}

				// handle unary rules
				boolean stop = false;
				while(!stop) {
					stop = true; // by default stop the loop (continue only if there was an improvement. not infinite since we add probs and check for minimum
					// check all unary rules
					for (String rhs : m_RHSindexedUnaryGrammar.keySet()) {
						Double childLogProb = probChart.get(row).get(start).get(rhs);
						// if the inserted symbol could be derived from an unary rule
						if (childLogProb != null) {
							// for all the unary rules lhs -> rhs
							for (String lhs : m_RHSindexedUnaryGrammar.get(rhs).keySet()) {
								Double oldLogProb = probChart.get(row).get(start).get(lhs);
								double ruleProb = m_RHSindexedUnaryGrammar.get(rhs).get(lhs);
								double newProb = childLogProb + ruleProb;
								// if never seen this symbol
								if (oldLogProb == null) {
									// continue to loop
									stop = false;
									// update probaility in the probChart - adding minus log probabilities
									probChart.get(row).get(start).put(lhs, newProb);
									// set according bpChart to the previous index (point to the last row, according word index and the symbol value)
									BackPointer bp = new BackPointer(new ChartIndex(row, start, rhs));
									bpChart.get(row).get(start).put(lhs, bp);
								}
								// else - we have already derived the symbol! we should check if we achieved improvement
								else {
									// compare probs. if it somehow improves continue loop
									if (newProb < oldLogProb) {
										// continue the loop
										stop = false;
										// update probChart
										probChart.get(row).get(start).put(lhs, newProb);
										// update backpointers entry
										BackPointer newBP = new BackPointer(new ChartIndex(row, start, rhs));
										bpChart.get(row).get(start).remove(lhs);
										bpChart.get(row).get(start).put(lhs, newBP);
									}
								} // end updating better entry
							}
						}  // end iterating over rules that derive rhs
					} // end iterating over unary rules
				} // end my misery
			} // end iterating over columns
		} // end iterating over rows
	} // end creating charts from syntactic rules !!!


	/**
	 * pre-process method that returns POS tag probabilites for terminals in given input
	 * @param input - list of strings (
	 * @return posTagsProbs - list of hashmaps according to possible pos tags and their probability
	 */
	private List<HashMap<String,Double>> lexicalizeInput(List<String> input) {
		List<HashMap<String,Double>> posTagsProbs = new ArrayList<>();
		for (int i = 0; i < input.size() ; i++) {
			String word = input.get(i);
			HashMap<String, Double> hm = new HashMap<>();
			Set<Rule> lexRules = m_mapLexicalRules.get(word);
			// if the terminal did'nt appear in the training - tag it as NN using the default NN -> UNK rule or use smoothing values for lhs->~UNK~ possibilities
			if (lexRules == null){
				for (Rule r : m_mapLexicalRules.get("~UNK~")){
					hm.put(r.getLHS().toString(),r.getMinusLogProb());
				}
			}
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

		@Override
		public String toString() {
			String s = "->" + child1.sym;
			if (child2 != null)
				s += child2.sym;
			return (s);
		}
	}

	private void addUnaryRules(){

	}


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

	// method gives a RHS indexed version of given grammar
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
