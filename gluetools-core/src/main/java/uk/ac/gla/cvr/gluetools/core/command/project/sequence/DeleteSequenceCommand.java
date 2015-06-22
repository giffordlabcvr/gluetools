package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="delete-sequence")
@CommandClass(description="Delete a sequence", 
	docoptUsages={"<sourceName> <sequenceID>"}) 
public class DeleteSequenceCommand extends ProjectModeCommand {

	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", true);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		GlueDataObject.delete(objContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID));
		objContext.commitChanges();
		return CommandResult.OK;
	}


}
