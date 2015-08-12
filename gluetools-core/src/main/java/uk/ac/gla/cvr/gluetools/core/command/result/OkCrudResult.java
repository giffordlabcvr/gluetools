package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.document.DocumentReader;
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
		super(mapBuilder()
				.put(OPERATION, operation.name())
				.put(OBJECT_TYPE, objectClass.getSimpleName())
				.put(NUMBER, number));
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		super.renderToConsoleAsText(renderCtx);
		DocumentReader documentReader = getDocumentReader();
		String number = documentReader.stringValue(NUMBER);
		String objectTypeString = documentReader.stringValue(OBJECT_TYPE);
		if(!number.equals("1")) {
			objectTypeString+="s";
		}
		String operationString = documentReader.stringValue(OPERATION).toLowerCase()+"d";
		renderCtx.output("("+number+" "+objectTypeString+" "+operationString+")");
	}
	
	public int getNumber() {
		return getDocumentReader().intValue(NUMBER);
	}
}
