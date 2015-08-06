package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete","sequence"}, 
	docoptUsages={"<sourceName> <sequenceID>"},
	docoptOptions={},
	description="Delete a sequence") 
public class DeleteSequenceCommand extends ProjectModeCommand<DeleteResult> {

	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", true);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		DeleteResult result = GlueDataObject.delete(cmdContext.getObjectContext(), 
				Sequence.class, Sequence.pkMap(sourceName, sequenceID));
		cmdContext.commit();
		return result;
	}


}
