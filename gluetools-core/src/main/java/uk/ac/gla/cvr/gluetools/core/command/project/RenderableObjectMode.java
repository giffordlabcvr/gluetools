package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public interface RenderableObjectMode extends InsideProjectMode {

	public GlueDataObject getRenderableObject(CommandContext cmdContext);
	
}
