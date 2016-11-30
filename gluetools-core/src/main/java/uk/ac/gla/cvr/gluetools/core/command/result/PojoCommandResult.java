package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;

public final class PojoCommandResult<D> extends CommandResult {

	public PojoCommandResult(D pojo) {
		super(PojoDocumentUtils.propertyNameForClass(pojo.getClass()));
		PojoDocumentUtils.setPojoProperties(getCommandDocument(), pojo);
	}

	
}
