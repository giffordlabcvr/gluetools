package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete","sequence"}, 
	docoptUsages={"[-s <sourceName>] <sequenceID>"},
	docoptOptions={"-s <sourceName>, --sourceName <sourceName>  Specify a particular source"},
	description="Delete a sequence") 
public class DeleteSequenceCommand extends ProjectModeCommand {

	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}

	// TODO sort out transactions properly here.
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Sequence sequence = lookupSequence(cmdContext, sourceName, sequenceID, false);
		objContext.deleteObject(sequence);
		return CommandResult.OK;
	}


}
