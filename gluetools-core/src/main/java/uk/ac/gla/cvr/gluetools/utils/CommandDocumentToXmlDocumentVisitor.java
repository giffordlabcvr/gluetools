package uk.ac.gla.cvr.gluetools.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
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
	
	public CommandDocumentToXmlDocumentVisitor() {
		this.document = GlueXmlUtils.newDocument();
	}
	
	@Override
	public void preVisitCommandDocument(CommandDocument documentBuilder) {
		Element rootElem = document.createElement(documentBuilder.getRootName());
		document.appendChild(rootElem);
		this.currentParentElem = rootElem;
	}

	@Override
	public void postVisitCommandObject(String objFieldName, CommandObject objectBuilder) {
		Node parentNode = this.currentParentElem.getParentNode();
		if(parentNode instanceof Element) {
			this.currentParentElem = (Element) parentNode;
		} else {
			this.currentParentElem = null;
		}
	}

	@Override
	public void visitCommandFieldValue(String objFieldName, CommandFieldValue commandFieldValue) {
		if(commandFieldValue instanceof SimpleCommandValue) {
			addSimpleValue(objFieldName, false, (SimpleCommandValue) commandFieldValue);
		} else if(commandFieldValue instanceof CommandObject) {
			addObjectValue(objFieldName, false, (CommandObject) commandFieldValue);
		} else if(commandFieldValue instanceof CommandArray) {
			CommandArray arrayBuilder = (CommandArray) commandFieldValue;
			arrayBuilder.accept(objFieldName, this);
		} 
	}
	
	@Override
	public void visitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {
		if(commandArrayItem instanceof SimpleCommandValue) {
			addSimpleValue(arrayFieldName, true, (SimpleCommandValue) commandArrayItem);
		} else if(commandArrayItem instanceof CommandObject) {
			addObjectValue(arrayFieldName, true, (CommandObject) commandArrayItem);
		} 
	}

	private void addObjectValue(String fieldName, boolean isMemberOfArray, CommandObject commandObject) {
		Element fieldElem = GlueXmlUtils.appendElement(currentParentElem, fieldName);
		CommandDocumentXmlUtils.setGlueType(fieldElem, GlueType.Object, isMemberOfArray);
		this.currentParentElem = fieldElem;
		commandObject.accept(fieldName, this);
	}

	private void addSimpleValue(String fieldName, boolean isMemberOfArray, SimpleCommandValue simpleCommandValue) {
		GlueType glueType = simpleCommandValue.getGlueType();
		Element fieldElem = GlueXmlUtils.appendElement(currentParentElem, fieldName);
		CommandDocumentXmlUtils.setGlueType(fieldElem, glueType, isMemberOfArray);
		if(glueType != GlueType.Null) {
			fieldElem.appendChild(document.createTextNode(glueType.renderAsString(simpleCommandValue.getValue())));
		}
	}

	public Document getXmlDocument() {
		return document;
	}

}
