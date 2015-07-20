package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"sequence"}, 
	docoptUsages={"<sourceName> <sequenceID>"},
	description="Enter command mode to manage a sequence") 
public class SequenceCommand extends ProjectModeCommand implements EnterModeCommand {

	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", true);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Sequence sequence = GlueDataObject.lookup(cmdContext.getObjectContext(), Sequence.class, 
				Sequence.pkMap(sourceName, sequenceID));
		Project project = getProjectMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new SequenceMode(project, sequence.getSource().getName(), sequenceID));
		return CommandResult.OK;
	}


}
