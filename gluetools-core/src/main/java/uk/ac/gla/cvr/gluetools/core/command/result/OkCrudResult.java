package uk.ac.gla.cvr.gluetools.core.command.result;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class OkCrudResult extends OkResult {

	public static final String OPERATION = "operation";
	public static final String NUMBER = "number";
	public static final String OBJECT_TYPE = "objectType";

	public enum Operation {
		CREATE,
		DELETE,
		UPDATE
	}
	
	public OkCrudResult(
			Operation operation,
			Class<? extends GlueDataObject> objectClass,
			int number) {
		super();
		getDocumentBuilder().set(OPERATION, operation.name());
		getDocumentBuilder().set(OBJECT_TYPE, objectClass.getSimpleName());
		getDocumentBuilder().set(NUMBER, number);
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		super.renderToConsoleAsText(renderCtx);
		Element docElem = getDocument().getDocumentElement();
		String number = GlueXmlUtils.getXPathElement(docElem, NUMBER).getTextContent();
		String objectTypeString = GlueXmlUtils.getXPathElement(docElem, OBJECT_TYPE).getTextContent();
		if(!number.equals("1")) {
			objectTypeString+="s";
		}
		String operationString = GlueXmlUtils.getXPathElement(docElem, OPERATION).
				getTextContent().toLowerCase()+"d";
		renderCtx.output("("+number+" "+objectTypeString+" "+operationString+")");
	}
	
	public int getNumber() {
		return Integer.parseInt(GlueXmlUtils.getXPathString(getDocument().getDocumentElement(), NUMBER+"/text()"));
	}
}
