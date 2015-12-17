package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup.GroupMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;


@CommandClass( 
	commandWords={"group"},
	docoptUsages={"<groupName>"},
	description="Enter command mode for a sequence group") 
@EnterModeCommandClass(
		commandModeClass = GroupMode.class)
public class GroupCommand extends ProjectModeCommand<OkResult>  {

	public static final String GROUP_NAME = "groupName";
	private String groupName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		groupName = PluginUtils.configureStringProperty(configElem, GROUP_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		SequenceGroup group = GlueDataObject.lookup(cmdContext, SequenceGroup.class, SequenceGroup.pkMap(groupName));
		cmdContext.pushCommandMode(new GroupMode(getProjectMode(cmdContext).getProject(), this, group.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends GroupNameCompleter {}
	

}
