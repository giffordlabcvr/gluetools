package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "reference"}, 
	docoptUsages={"<refSequenceName>"},
	description="Delete a reference sequence", 
	furtherHelp="Deletion of a reference sequence does not cause the deletion of its sequence.") 
public class DeleteReferenceSequenceCommand extends ProjectModeCommand {

	private String refSequenceName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSequenceName = PluginUtils.configureStringProperty(configElem, "refSequenceName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		return GlueDataObject.delete(objContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSequenceName));
	}

	@CompleterClass
	public static class Completer extends RefSeqNameCompleter {}

}
