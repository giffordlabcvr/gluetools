/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.scripting;

import java.util.ArrayList;
import java.util.Date;
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
import uk.ac.gla.cvr.gluetools.utils.DateUtils;

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
			Object value = ((SimpleCommandValue) commandFieldValue).getValue();
			if(value instanceof Date) {
				value = DateUtils.render((Date) value);
			}
			currentMap().put(objFieldName, value);
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
			Object value = ((SimpleCommandValue) commandArrayItem).getValue();
			if(value instanceof Date) {
				value = DateUtils.render((Date) value);
			}
			currentList().add(value);
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
