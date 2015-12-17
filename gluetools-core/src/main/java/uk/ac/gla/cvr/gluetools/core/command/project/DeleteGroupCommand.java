package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;


@CommandClass( 
	commandWords={"delete", "group"}, 
	docoptUsages={"<groupName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a sequence group", 
	furtherHelp="Deletion of a group does not cause the deletion of its member sequences.") 
public class DeleteGroupCommand extends ProjectModeCommand<DeleteResult> {

	private String groupName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		groupName = PluginUtils.configureStringProperty(configElem, "groupName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		DeleteResult result = GlueDataObject.delete(cmdContext, SequenceGroup.class, SequenceGroup.pkMap(groupName), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends GroupNameCompleter {}

}
