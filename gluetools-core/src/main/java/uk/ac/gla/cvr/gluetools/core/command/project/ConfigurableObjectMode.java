package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;

public interface ConfigurableObjectMode extends InsideProjectMode {

	public ConfigurableTable getConfigurableTable();
	
	public GlueDataObject getConfigurableObject(CommandContext cmdContext);
	
}
