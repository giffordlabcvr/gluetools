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
package uk.ac.gla.cvr.gluetools.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.document.CommandArrayItem;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocumentVisitor;
import uk.ac.gla.cvr.gluetools.core.document.CommandFieldValue;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.SimpleCommandValue;
import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils.GlueType;

public class CommandDocumentToXmlDocumentVisitor implements CommandDocumentVisitor {

	private Document document;
	private Element currentParentElem;
	
	private boolean addGlueTypes = true;
	private boolean includeGlueNullElements = true;

	public CommandDocumentToXmlDocumentVisitor() {
		this.document = GlueXmlUtils.newDocument();
	}
	
	public void setAddGlueTypes(boolean addGlueTypes) {
		this.addGlueTypes = addGlueTypes;
	}

	public void setIncludeGlueNullElements(boolean includeGlueNullElements) {
		this.includeGlueNullElements = includeGlueNullElements;
	}

	@Override
	public void preVisitCommandDocument(CommandDocument documentBuilder) {
		Element rootElem = document.createElement(documentBuilder.getRootName());
		document.appendChild(rootElem);
		this.currentParentElem = rootElem;
	}


	@Override
	public void preVisitCommandFieldValue(String objFieldName, CommandFieldValue commandFieldValue) {
		if(commandFieldValue instanceof SimpleCommandValue) {
			addSimpleValue(objFieldName, false, (SimpleCommandValue) commandFieldValue);
		} else if(commandFieldValue instanceof CommandObject) {
			addObjectValue(objFieldName, false);
		} 
	}

	@Override
	public void postVisitCommandFieldValue(String objFieldName, CommandFieldValue commandFieldValue) {
		if(commandFieldValue instanceof CommandObject) {
			Node parentNode = this.currentParentElem.getParentNode();
			if(parentNode instanceof Element) {
				this.currentParentElem = (Element) parentNode;
			} else {
				this.currentParentElem = null;
			}
		} 
	}
	
	@Override
	public void preVisitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {
		if(commandArrayItem instanceof SimpleCommandValue) {
			addSimpleValue(arrayFieldName, true, (SimpleCommandValue) commandArrayItem);
		} else if(commandArrayItem instanceof CommandObject) {
			addObjectValue(arrayFieldName, true);
		} 
	}

	@Override
	public void postVisitCommandArrayItem(String objFieldName, CommandArrayItem commandArrayItem) {
		if(commandArrayItem instanceof CommandObject) {
			Node parentNode = this.currentParentElem.getParentNode();
			if(parentNode instanceof Element) {
				this.currentParentElem = (Element) parentNode;
			} else {
				this.currentParentElem = null;
			}
		} 
	}
	
	private void addObjectValue(String fieldName, boolean isMemberOfArray) {
		Element fieldElem = GlueXmlUtils.appendElement(currentParentElem, fieldName);
		if(addGlueTypes) {
			CommandDocumentXmlUtils.setGlueType(fieldElem, GlueType.Object, isMemberOfArray);
		}
		this.currentParentElem = fieldElem;
	}

	private void addSimpleValue(String fieldName, boolean isMemberOfArray, SimpleCommandValue simpleCommandValue) {
		GlueType glueType = simpleCommandValue.getGlueType();
		if(glueType != GlueType.Null || includeGlueNullElements) {
			Element fieldElem = GlueXmlUtils.appendElement(currentParentElem, fieldName);
			if(addGlueTypes) {
				CommandDocumentXmlUtils.setGlueType(fieldElem, glueType, isMemberOfArray);
			}
			if(glueType != GlueType.Null) {
				fieldElem.appendChild(document.createTextNode(glueType.renderAsString(simpleCommandValue.getValue())));
			}
		}
	}

	public Document getXmlDocument() {
		return document;
	}

}
