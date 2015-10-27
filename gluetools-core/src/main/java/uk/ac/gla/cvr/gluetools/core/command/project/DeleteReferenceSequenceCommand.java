package uk.ac.gla.cvr.gluetools.core.command.project;

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
	docoptUsages={"<refSeqName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a reference sequence", 
	furtherHelp="Deletion of a reference sequence does not cause the deletion of its sequence.") 
public class DeleteReferenceSequenceCommand extends ProjectModeCommand<DeleteResult> {

	public static final String REF_SEQ_NAME = "refSeqName";
	private String refSeqName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, REF_SEQ_NAME, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		DeleteResult result = GlueDataObject.delete(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends RefSeqNameCompleter {}

}
