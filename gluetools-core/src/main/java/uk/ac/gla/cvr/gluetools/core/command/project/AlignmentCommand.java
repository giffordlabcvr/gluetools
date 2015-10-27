package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"alignment"},
	docoptUsages={"<alignmentName>"},
	description="Enter command mode for an alignment") 
@EnterModeCommandClass(
		commandModeClass = AlignmentMode.class)
public class AlignmentCommand extends ProjectModeCommand<OkResult>  {

	public static final String ALIGNMENT_NAME = "alignmentName";
	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		cmdContext.pushCommandMode(new AlignmentMode(getProjectMode(cmdContext).getProject(), this, alignment.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends AlignmentNameCompleter {}
	

}
