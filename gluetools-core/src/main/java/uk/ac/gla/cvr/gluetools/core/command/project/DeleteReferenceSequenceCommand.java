package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "reference"}, 
	docoptUsages={"<referenceName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a reference sequence", 
	furtherHelp="Deletion of a reference sequence does not cause the deletion of its sequence.") 
public class DeleteReferenceSequenceCommand extends ProjectModeCommand<DeleteResult> {

	public static final String REFERENCE_NAME = "referenceName";
	private String referenceName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		DeleteResult result = GlueDataObject.delete(objContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName));
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends RefSeqNameCompleter {}

}
