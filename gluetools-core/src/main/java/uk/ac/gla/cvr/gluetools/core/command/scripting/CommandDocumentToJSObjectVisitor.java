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
import java.util.LinkedHashMap;
import java.util.LinkedList;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandArrayItem;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocumentVisitor;
import uk.ac.gla.cvr.gluetools.core.document.CommandFieldValue;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.SimpleCommandValue;

// this class was implemented in the hope that it would
// generate "native" JS objects which would be amenable to JSON.stringify.
// However this proved not to be the case. So this class is unused.
// Instead CommandDocumentToMapVisitor is used and the translation to native JS 
// objects is done inside glue.js
public class CommandDocumentToJSObjectVisitor implements CommandDocumentVisitor {

	private MapJSObject rootMap;
	private LinkedList<Object> stack = new LinkedList<Object>();
	
	@Override
	public void preVisitCommandDocument(CommandDocument commandDocument) {
		rootMap = new MapJSObject(new LinkedHashMap<String, Object>());
		stack.push(rootMap);
	}
	
	@Override
	public void postVisitCommandDocument(CommandDocument commandDocument) {
		stack.pop();
	}

	@Override
	public void preVisitCommandObject(String objFieldName, CommandObject commandObject) {
		MapJSObject map = new MapJSObject(new LinkedHashMap<String, Object>());
		if(!stack.isEmpty()) {
			Object topOfStack = stack.peek();
			if(topOfStack instanceof MapJSObject) {
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
		ListJSObject arrayList = new ListJSObject(new ArrayList<Object>());
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
	private MapJSObject currentMap() {
		return (MapJSObject) stack.peek();
	}

	@SuppressWarnings("unchecked")
	private ListJSObject currentList() {
		return (ListJSObject) stack.peek();
	}
	public MapJSObject getRootMap() {
		return rootMap;
	}

	
	
}
