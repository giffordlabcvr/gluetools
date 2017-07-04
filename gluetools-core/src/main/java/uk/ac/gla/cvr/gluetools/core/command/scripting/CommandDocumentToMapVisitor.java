package uk.ac.gla.cvr.gluetools.core.command.scripting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandArrayItem;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocumentVisitor;
import uk.ac.gla.cvr.gluetools.core.document.CommandFieldValue;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.SimpleCommandValue;

public class CommandDocumentToMapVisitor implements CommandDocumentVisitor {

	private Map<String, Object> rootMap;
	private LinkedList<Object> stack = new LinkedList<Object>();
	
	@Override
	public void preVisitCommandDocument(CommandDocument commandDocument) {
		rootMap = new LinkedHashMap<String, Object>();
		stack.push(rootMap);
	}
	
	@Override
	public void postVisitCommandDocument(CommandDocument commandDocument) {
		stack.pop();
	}

	@Override
	public void preVisitCommandObject(String objFieldName, CommandObject commandObject) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		if(!stack.isEmpty()) {
			Object topOfStack = stack.peek();
			if(topOfStack instanceof Map) {
				currentMap().put(objFieldName, map);
			} else {
				currentList().add(map);
			}
		}
		stack.push(map);
	}

	@Override
	public void postVisitCommandObject(String objFieldName, CommandObject commandObject) {
		stack.pop();
	}

	@Override
	public void preVisitCommandFieldValue(String objFieldName, CommandFieldValue commandFieldValue) {
		if(commandFieldValue instanceof SimpleCommandValue) {
			currentMap().put(objFieldName, ((SimpleCommandValue) commandFieldValue).getValue());
		}
	}

	@Override
	public void preVisitCommandArray(String arrayFieldName, CommandArray commandArray) {
		List<Object> arrayList = new ArrayList<Object>();
		currentMap().put(arrayFieldName, arrayList);
		stack.push(arrayList);
	}

	@Override
	public void postVisitCommandArray(String arrayFieldName, CommandArray commandArray) {
		stack.pop();
	}

	@Override
	public void preVisitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {
		if(commandArrayItem instanceof SimpleCommandValue) {
			currentList().add(((SimpleCommandValue) commandArrayItem).getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> currentMap() {
		return (Map<String, Object>) stack.peek();
	}

	@SuppressWarnings("unchecked")
	private List<Object> currentList() {
		return (List<Object>) stack.peek();
	}
	public Map<String, Object> getRootMap() {
		return rootMap;
	}

	
	
}
