package uk.ac.gla.cvr.gluetools.core.docopt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.docopt.DocoptLexer.Token;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptLexer.TokenType;

public class DocoptFSM {

	
	public static Node buildFSM(List<Token> usageTokens, Map<Character, String> optionMap) {
		Node startNode = new Node();
		LinkedList<Context> contextStack = new LinkedList<Context>();
		Context context = new Context(startNode, "nothing");
		context.startNode = startNode;
		contextStack.push(context);
		buildFSM(startNode, contextStack, new LinkedList<Token>(usageTokens), optionMap);
		return startNode;
	}


	private static void buildFSM(Node startNode, LinkedList<Context> contextStack, LinkedList<Token> tokens, Map<Character, String> optionMap) {
		Node currentNode = startNode;
		Node lastNode = null;
		TokenType lastType = null;
		while(!tokens.isEmpty()) {
			Token currentToken = tokens.pop();
			TokenType currentType = currentToken.getType();
			switch(currentType) {
			case LITERAL:
				currentNode = currentNode.literalToNew(currentToken.getData());
				break;
			case VARIABLE:
				String variableName = currentToken.getData().replace("<", "").replace(">", "");
				currentNode = currentNode.variableToNew(variableName);
				break;
			case BRLEFT:
				contextStack.push(new Context(currentNode, ")"));
				break;
			case BRRIGHT: {
				Context context = contextStack.pop();
				String expectBracket = context.expectBracket;
				if(!expectBracket.equals(")")) {
					throw new RuntimeException("Mismatched parentheses: found \")\", expecting "+expectBracket);
				}
				context.options.add(currentNode);
				Node endNode = new Node();
				context.options.forEach(optionNode -> optionNode.nullTo(endNode));
				currentNode = endNode;
			}
			break;
			case SQLEFT:
				contextStack.push(new Context(currentNode, "]"));
				break;
			case SQRIGHT: {
				Context context = contextStack.pop();
				String expectBracket = context.expectBracket;
				if(!expectBracket.equals("]")) {
					throw new RuntimeException("Mismatched parentheses: found \"]\", expecting "+expectBracket);
				}
				context.options.add(currentNode);
				Node endNode = new Node();
				context.options.forEach(optionNode -> optionNode.nullTo(endNode));
				context.startNode.nullTo(endNode);
				currentNode = endNode;
			}
			break;
			case PIPE:
				Context context = contextStack.peek();
				context.options.add(currentNode);
				currentNode = context.startNode;
				break;
			case ELLIPSIS:
				if(lastType != TokenType.VARIABLE) {
					throw new RuntimeException("Ellipsis can only occur after variable");
				}
				currentNode.nullTo(lastNode);
				break;
			case OPTION:
				String optionWithMinus = currentToken.getData();
				Node nextNode = currentNode.literalToNew(optionWithMinus);
				Character optionLetter = optionWithMinus.charAt(1);
				String optionWord = optionMap.get(optionLetter);
				if(optionWord == null) {
					throw new RuntimeException("Missing option documentation: "+optionWithMinus);
				}
				currentNode.literalTo(nextNode, "--"+optionWord);
				currentNode = nextNode;
				break;
			default:
			}
			lastNode = currentNode;
			lastType = currentType;
		}
	}


	private static class Context {
		Node startNode;
		String expectBracket;
		List<Node> options = new ArrayList<Node>();
		
		public Context(Node startNode, String expectBracket) {
			super();
			this.startNode = startNode;
			this.expectBracket = expectBracket;
		}
	
	}
	
	
	
	
	public static class Node {
		private List<Transition> transitions = new ArrayList<Transition>();
		public List<Transition> getTransitions() {
			return transitions;
		}
		public void nullTo(Node toNode) {
			transitions.add(new NullTransition(toNode));
		}
		public Node nullToNew() {
			Node newNode = new Node();
			nullTo(newNode);
			return newNode;
		}
		public void variableTo(Node toNode, String variableName) {
			transitions.add(new VariableTransition(toNode, variableName));
		}
		public Node variableToNew(String variableName) {
			Node newNode = new Node();
			variableTo(newNode, variableName);
			return newNode;
		}
		public void literalTo(Node toNode, String literal) {
			transitions.add(new LiteralTransition(toNode, literal));
		}
		public Node literalToNew(String literal) {
			Node newNode = new Node();
			literalTo(newNode, literal);
			return newNode;
		}

		public List<Transition> nonNullTransitions() {
			Set<Node> frontier = new LinkedHashSet<Node>(); // nodes which can be reached from here via zero or more null transitions.
			LinkedList<Node> queue = new LinkedList<Node>();
			queue.add(this);
			while(!queue.isEmpty()) {
				Node current = queue.pop();
				frontier.add(current);
				queue.addAll(current.transitions
						.stream()
						.filter(t -> { return t instanceof NullTransition; })
						.map(nt -> { return nt.getToNode(); })
						.filter(n -> {return !frontier.contains(n);})
						.collect(Collectors.toList()));
			}
			return frontier
					.stream()
					.map(n -> { return n.transitions; })
					.flatMap(List::stream)
					.filter(t -> { return !(t instanceof NullTransition); })
					.collect(Collectors.toList());
			
		}

	
	
	}
	
	public abstract static class Transition {
		private Node toNode;
		public Node getToNode() {
			return toNode;
		}
		public Transition(Node toNode) {
			super();
			this.toNode = toNode;
		}
	}
	
	public static class NullTransition extends Transition {

		public NullTransition(Node toNode) {
			super(toNode);
		}
	}

	public static class LiteralTransition extends Transition {
		private String literal;
		public LiteralTransition(Node toNode, String literal) {
			super(toNode);
			this.literal = literal;
		}
		public String getLiteral() {
			return literal;
		}
	}

	public static class VariableTransition extends Transition {
		private String variableName;
		public VariableTransition(Node toNode, String variableName) {
			super(toNode);
			this.variableName = variableName;
		}
		public String getVariableName() {
			return variableName;
		}
	}

}
