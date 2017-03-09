package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public class CountResult extends MapResult {

	public static final String COUNT_RESULT = "countResult";
	public static final String COUNT = "count";
	public static final String OBJECT_TYPE = "objectType";


	public <D extends GlueDataObject> CountResult(CommandContext cmdContext, Class<D> objectClass, int count) {
		super(COUNT_RESULT, mapBuilder().put(COUNT, new Integer(count)));
		getCommandDocument().set(OBJECT_TYPE, objectClass.getSimpleName());
	}
	
	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		String objectType = getCommandDocument().getString(OBJECT_TYPE);
		int count = getCommandDocument().getInteger(COUNT);
		renderCtx.output(objectType+"s found: "+count);
	}

}
