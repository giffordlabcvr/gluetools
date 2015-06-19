package uk.ac.gla.cvr.gluetools.core.collation.populating;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

/**
 * A plugin that can obtain collated sequences from a source.
 * 
 */
public interface PopulatorPlugin extends Plugin {

	public CommandResult populate(CommandContext cmdContext, String sourcename);
	
	
}
