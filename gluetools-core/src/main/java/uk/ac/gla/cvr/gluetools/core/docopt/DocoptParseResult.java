package uk.ac.gla.cvr.gluetools.core.docopt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.LiteralTransition;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.Node;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.Transition;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.VariableTransition;

public class DocoptParseResult {

	private Map<String, Object> bindings = new LinkedHashMap<String, Object>();
	private List<String> nextLiterals = new ArrayList<String>();
	private String nextVariable;
	
	public Map<String, Object> getBindings() {
		return bindings;
	}

	public List<String> getNextLiterals() {
		return nextLiterals;
	}

	public String getNextVariable() {
		return nextVariable;
	}

	private DocoptParseResult() {
	}

	public static DocoptParseResult parse(List<String> args, Map<Character, String> optionsMap, Node startNode) {
		Node currentNode = startNode;
		DocoptParseResult result = new DocoptParseResult();
		for(String arg: args) {
			List<Transition> nonNullTransitions = currentNode.nonNullTransitions();
			if(nonNullTransitions.isEmpty()) {
				return result;
			}
			for(Transition transition: nonNullTransitions) {
				if(transition instanceof LiteralTransition) {
					String literal = ((LiteralTransition) transition).getLiteral();
					if(literal.equals(arg)) {
						if(literal.startsWith("--")) {
							result.bindings.put(literal.replace("--", ""), true);
						} else if(literal.startsWith("-")) {
							result.bindings.put(optionsMap.get(literal.charAt(1)), true);
						}
						currentNode = transition.getToNode();
						break;
					}
				} else if(transition instanceof VariableTransition) {
					String variableName = ((VariableTransition) transition).getVariableName();
					Object currentBinding = result.bindings.get(variableName);
					if(currentBinding == null) {
						result.bindings.put(variableName, arg);
					} else if(currentBinding instanceof String) {
						ArrayList<String> newBinding = new ArrayList<String>();
						newBinding.add((String) currentBinding);
						newBinding.add(arg);
						result.bindings.put(variableName, newBinding);
					} else if(currentBinding instanceof ArrayList<?>) {
						@SuppressWarnings("unchecked")
						ArrayList<String> arrayList = (ArrayList<String>) currentBinding;
						arrayList.add(arg);
					} else if(currentBinding instanceof Boolean && ((Boolean) currentBinding)) {
						result.bindings.put(variableName, arg);
					}
					currentNode = transition.getToNode();
					break;
				}
			}
			
		}
		List<Transition> finalNonNullTransitions = currentNode.nonNullTransitions();
		for(Transition finalTransition: finalNonNullTransitions) {
			if(finalTransition instanceof LiteralTransition) {
				String literal = ((LiteralTransition) finalTransition).getLiteral();
				result.nextLiterals.add(literal);
			} else if(finalTransition instanceof VariableTransition) {
				String variableName = ((VariableTransition) finalTransition).getVariableName();
				result.nextVariable = variableName;
			}	
		}
		// System.out.println("nonNullTransitions: "+finalNonNullTransitions);
		return result;
	}

	
}
