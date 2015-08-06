package uk.ac.gla.cvr.gluetools.core.document;

import javax.json.JsonObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

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

	
	public Document getXmlDocument() {
		return getElement().getOwnerDocument();
	}

	public JsonObject getJsonObject() {
		return JsonUtils.documentToJSonObjectBuilder(getXmlDocument()).build();
	}

}
