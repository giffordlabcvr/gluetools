package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;


@CommandClass( 
	commandWords={"create","group"}, 
	docoptUsages={"<groupName>"},
	description="Create a new sequence group", 
	metaTags={CmdMeta.updatesDatabase}
	) 
public class CreateGroupCommand extends ProjectModeCommand<CreateResult> {

	public static final String GROUP_NAME = "groupName";
	
	private String groupName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		groupName = PluginUtils.configureStringProperty(configElem, GROUP_NAME, false);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		SequenceGroup group = GlueDataObject.create(cmdContext, SequenceGroup.class, SequenceGroup.pkMap(groupName), false);
		group.setLastUpdateTime(System.currentTimeMillis());
		cmdContext.commit();
		return new CreateResult(SequenceGroup.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}
}
