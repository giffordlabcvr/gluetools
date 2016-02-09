package uk.ac.gla.cvr.gluetools.core.digs;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;

@CommandClass(
		commandWords={"build", "target", "group"}, 
		description = "Ensure the BLAST database for a target sequence group is up-to-date", 
		docoptUsages = { "<groupName>" },
		metaTags = {}	
)
public class BuildTargetGroupCommand extends ModulePluginCommand<OkResult, DigsProber> implements ProvidedProjectModeCommand {

	private static final String GROUP_NAME = "groupName";

	private String groupName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.groupName = PluginUtils.configureStringProperty(configElem, GROUP_NAME, true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, DigsProber digsProber) {
		BlastDbManager.getInstance().ensureSequenceGroupDB(cmdContext, groupName);
		return new OkResult();
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("groupName", SequenceGroup.class, SequenceGroup.NAME_PROPERTY);
		}
		
	}
	
}
