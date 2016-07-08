package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.NewickLexer.Token;

public class NewickToPhyloTreeParser {

	
	public NewickToPhyloTreeParser() {
		
	}

	public PhyloTree parseNewick(String newickString) {
		ArrayList<Token> tokens = NewickLexer.lex(newickString);
		List<Token> meaningfulTokens = NewickLexer.meaningfulTokens(tokens);

		LinkedList<State> stateStack = new LinkedList<State>();
		TreeState treeState = new TreeState();
		push(stateStack, treeState);
		
		for(Token token: meaningfulTokens) {
			//System.out.println("CONSUME: "+token);
			stateStack.peek().consume(stateStack, token);
		}
		if(!stateStack.isEmpty()) {
			throw new PhyloNewickException(PhyloNewickException.Code.PARSE_ERROR);
		}
		return treeState.phyloTree;
	}
	
	
	private abstract class State {
		public void consume(LinkedList<State> stateStack, Token token) {
			throw new PhyloNewickException(PhyloNewickException.Code.UNEXPECTED_NEWICK_TOKEN, token);
		}
	}
	
	private void assertTrue(boolean proposition) {
		if(!proposition) {
			throw new PhyloNewickException(PhyloNewickException.Code.PARSE_ERROR);
		}
	}

	private void push(LinkedList<State> stateStack, State state) {
		//System.out.println("PUSH: "+state.getClass().getSimpleName());
		stateStack.push(state);
	}
	
	private <S extends State> S pop(LinkedList<State> stateStack, Class<S> theClass) {
		assertTrue(!stateStack.isEmpty());
		State state = stateStack.pop();
		//System.out.println("POP: "+state.getClass().getSimpleName());
		assertTrue(theClass.isAssignableFrom(state.getClass()));
		return theClass.cast(state);
	}

	private <S extends State> S peek(LinkedList<State> stateStack, Class<S> theClass) {
		assertTrue(!stateStack.isEmpty());
		State state = stateStack.peek();
		assertTrue(theClass.isAssignableFrom(state.getClass()));
		return theClass.cast(state);
	}

	
	private class TreeState extends State {
		PhyloTree phyloTree = new PhyloTree();

		@Override
		public void consume(LinkedList<State> stateStack, Token token) {
			switch(token.getType()) {
			case LEFTPAREN:
				assertTrue(phyloTree.getRoot() == null);
				InternalState internalState = new InternalState();
				phyloTree.setRoot(internalState.internal);
				push(stateStack, internalState);
				internalState.consume(stateStack, token);
				break;
			case NAME:
				assertTrue(phyloTree.getRoot() == null);
				LeafState leafState = new LeafState();
				phyloTree.setRoot(leafState.leaf);
				push(stateStack, leafState);
				leafState.consume(stateStack, token);
				break;
			case SEMICOLON:
				if(phyloTree.getRoot() == null) {
					phyloTree.setRoot(new PhyloLeaf());
				}
				pop(stateStack, TreeState.class);
				break;
			default:
				throw new PhyloNewickException(PhyloNewickException.Code.PARSE_ERROR);
			}
		}

	}
	
	private class InternalState extends State {
		PhyloInternal internal = new PhyloInternal();
		boolean allBranches = false;

		@Override
		public void consume(LinkedList<State> stateStack, Token token) {
			switch(token.getType()) {
			case LEFTPAREN:
				assertTrue(internal.getBranches().size() == 0); 
				assertTrue(!allBranches);
				{
					BranchState branchState = new BranchState();
					push(stateStack, branchState);
					internal.addBranch(branchState.branch);
				}
				break;
			case COMMA:
				assertTrue(internal.getBranches().size() > 0);
				assertTrue(!allBranches);
				{
					BranchState branchState = new BranchState();
					push(stateStack, branchState);
					internal.addBranch(branchState.branch);
				}
				break;
			case RIGHTPAREN:
				assertTrue(internal.getBranches().size() > 0);
				if(allBranches) {
					pop(stateStack, InternalState.class);
					peek(stateStack, BranchState.class).consume(stateStack, token);
				} else {
					allBranches = true;
				}
				break;
			case NAME:
				assertTrue(allBranches);
				internal.setName(token.getData());
				pop(stateStack, InternalState.class);
				break;
			case COLON:
				assertTrue(allBranches);
				pop(stateStack, InternalState.class);
				peek(stateStack, BranchState.class).consume(stateStack, token);
				break;
			case SEMICOLON:
				assertTrue(allBranches);
				pop(stateStack, InternalState.class);
				peek(stateStack, TreeState.class).consume(stateStack, token);
				break;
			default:
				super.consume(stateStack, token);
			}
		}
	}

	private class BranchState extends State {
		PhyloBranch branch = new PhyloBranch();
		boolean colon = false;
		boolean number = false;

		@Override
		public void consume(LinkedList<State> stateStack, Token token) {
			switch(token.getType()) {
			case LEFTPAREN:
				assertTrue(branch.getSubtree() == null);
				assertTrue(!colon);
				InternalState internalState = new InternalState();
				branch.setSubtree(internalState.internal);
				push(stateStack, internalState);
				internalState.consume(stateStack, token);
				break;
			case NAME:
				assertTrue(branch.getSubtree() == null);
				assertTrue(!colon);
				LeafState leafState = new LeafState();
				branch.setSubtree(leafState.leaf);
				push(stateStack, leafState);
				leafState.consume(stateStack, token);
				break;
			case COLON:
				assertTrue(!colon);
				if(branch.getSubtree() == null) {
					branch.setSubtree(new PhyloLeaf());
				}
				colon = true;
				break;
			case NUMBER: 
				assertTrue(colon);
				assertTrue(!number);
				branch.setLength(Double.parseDouble(token.getData()));
				number = true;
				pop(stateStack, BranchState.class);
				break;
			case COMMA: 
			case RIGHTPAREN: 
				assertTrue(number || !colon);
				if(branch.getSubtree() == null) {
					branch.setSubtree(new PhyloLeaf());
				}
				pop(stateStack, BranchState.class);
				peek(stateStack, InternalState.class).consume(stateStack, token);;
				break;
			default:
				throw new PhyloNewickException(PhyloNewickException.Code.PARSE_ERROR);
			}
		}

	}

	
	private class LeafState extends State {
		PhyloLeaf leaf = new PhyloLeaf();

		@Override
		public void consume(LinkedList<State> stateStack, Token token) {
			switch(token.getType()) {
			case NAME:
				assertTrue(leaf.getName() == null);
				leaf.setName(token.getData());
				pop(stateStack, LeafState.class);
				break;
			case SEMICOLON:
				pop(stateStack, LeafState.class);
				peek(stateStack, TreeState.class).consume(stateStack, token);
				break;
			case COMMA:
			case RIGHTPAREN:
				pop(stateStack, LeafState.class);
				peek(stateStack, InternalState.class).consume(stateStack, token);
				break;
			default:
				super.consume(stateStack, token);
			}
		}
	}

	private void test(String input) {
		System.out.println("Input:  "+input);
		PhyloTree result = parseNewick(input);
		System.out.println("Output: "+PhyloNewickUtils.phyloTreeToNewick(result));
		System.out.println("------");
	}

	
	public static void main(String[] args) {
		NewickToPhyloTreeParser parser = new NewickToPhyloTreeParser();
		parser.test("(,,(,));");
		parser.test("(A,B,(C,D));");
		parser.test("(A,B,(C,D)E)F;");
		parser.test("(:0.1,:0.2,(:0.3,:0.4):0.5);");
		// we don't support the Tree -> Branch ; production, exemplified by this case.
		// parser.test("(:0.1,:0.2,(:0.3,:0.4):0.5):0.0;");
		parser.test("(A:0.1,B:0.2,(C:0.3,D:0.4):0.5);");
		parser.test("(A:0.1,B:0.2,(C:0.3,D:0.4)E:0.5)F;");
		parser.test("((B:0.2,(C:0.3,D:0.4)E:0.5)F:0.1)A;");
	}

	
	
}
