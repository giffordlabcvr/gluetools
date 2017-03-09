package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;

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
			Class<?> objectClass,
			int number) {
		super(mapBuilder()
				.put(OPERATION, operation.name())
				.put(OBJECT_TYPE, objectClass.getSimpleName())
				.put(NUMBER, number));
	}

	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		super.renderToConsoleAsText(renderCtx);
		CommandDocument commandDocument = getCommandDocument();
		int number = commandDocument.getInteger(NUMBER);
		String objectTypeString = commandDocument.getString(OBJECT_TYPE);
		if(number != 1) {
			objectTypeString+="s";
		}
		String operationString = commandDocument.getString(OPERATION).toLowerCase()+"d";
		renderCtx.output("("+number+" "+objectTypeString+" "+operationString+")");
	}
	
	public int getNumber() {
		return getCommandDocument().getInteger(NUMBER);
	}
}
