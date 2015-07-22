package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"member"},
	docoptUsages={"<sourceName> <sequenceID>"},
	description="Enter command mode for an alignment member") 
public class MemberCommand extends AlignmentModeCommand implements EnterModeCommand {

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
		// check existence.
		GlueDataObject.lookup(cmdContext.getObjectContext(), Sequence.class, 
				Sequence.pkMap(sourceName, sequenceID));
		Project project = getAlignmentMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new MemberMode(project, sourceName, sequenceID));
		return CommandResult.OK;
	}

}
