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
		CommandDocumentXmlUtils.setGlueType(fieldElem, GlueType.Object, isMemberOfArray);
		this.currentParentElem = fieldElem;
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
