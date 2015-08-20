package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "alignment"}, 
	docoptUsages={"<alignmentName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete an alignment", 
	furtherHelp="Deletion of an alignment does not cause the deletion of its reference or member sequences.") 
public class DeleteAlignmentCommand extends ProjectModeCommand<DeleteResult> {

	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		DeleteResult result = GlueDataObject.delete(objContext, Alignment.class, Alignment.pkMap(alignmentName));
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends AlignmentNameCompleter {}

}
