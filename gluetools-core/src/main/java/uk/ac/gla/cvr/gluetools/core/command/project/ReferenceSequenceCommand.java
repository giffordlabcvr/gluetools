package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceSequenceMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"reference"},
	docoptUsages={"<refSeqName>"},
	description="Enter command mode for a reference sequence") 
public class ReferenceSequenceCommand extends ProjectModeCommand implements EnterModeCommand {

	private String refSeqName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, "refSeqName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		ReferenceSequence refSequence = GlueDataObject.lookup(objContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName));
		cmdContext.pushCommandMode(new ReferenceSequenceMode(getProjectMode(cmdContext).getProject(), refSequence.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends RefSeqNameCompleter {}
	

}
