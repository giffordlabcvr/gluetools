package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"alignment"},
	docoptUsages={"<alignmentName>"},
	description="Enter command mode for an alignment") 
public class AlignmentCommand extends ProjectModeCommand implements EnterModeCommand {

	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment alignment = GlueDataObject.lookup(objContext, Alignment.class, Alignment.pkMap(alignmentName));
		cmdContext.pushCommandMode(new AlignmentMode(cmdContext, alignment.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends AlignmentNameCompleter {}
	

}
