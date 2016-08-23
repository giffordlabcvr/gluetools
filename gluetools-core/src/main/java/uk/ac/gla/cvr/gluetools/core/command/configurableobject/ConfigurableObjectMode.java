package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public interface ConfigurableObjectMode extends InsideProjectMode {

	public String getTableName();
	
	public GlueDataObject getConfigurableObject(CommandContext cmdContext);
	
}
