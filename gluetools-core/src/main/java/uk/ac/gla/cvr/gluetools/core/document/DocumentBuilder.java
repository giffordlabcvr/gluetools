package uk.ac.gla.cvr.gluetools.core.document;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class DocumentBuilder extends ObjectBuilder {


	public DocumentBuilder(String name) {
		super(createDocElem(name), false);
	}

	private static Element createDocElem(String name) {
		Document document = GlueXmlUtils.newDocument();
		Element rootElem = document.createElement(name);
		document.appendChild(rootElem);
		return rootElem;
	}

	
	public Document getDocument() {
		return getElement().getOwnerDocument();
	}

}
