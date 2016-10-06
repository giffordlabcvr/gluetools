package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"descendent-tree"},
		docoptUsages={"[-s <sortProperties>]"},
		docoptOptions={
			"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties"},
		furtherHelp=
				"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
				"This is applied when sorting child alignments.\n",
		description="Render the descendents of this alignment as a tree"
	) 
public class AlignmentDescendentTreeCommand extends AlignmentModeCommand<AlignmentDescendentTreeResult> {

	public static final String SORT_PROPERTIES = "sortProperties";
	
	private String sortProperties;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.sortProperties = PluginUtils.configureStringProperty(configElem, SORT_PROPERTIES, false);
	}



	@Override
	public AlignmentDescendentTreeResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		return new AlignmentDescendentTreeResult(cmdContext, alignment, sortProperties);
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}

}
