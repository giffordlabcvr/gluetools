package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class AlignmentModeCommand<R extends CommandResult> extends Command<R> {

	public static final String ALIGNMENT_NAME = "alignmentName";


	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
	}

	protected String getAlignmentName() {
		return alignmentName;
	}

	protected AlignmentMode getAlignmentMode(CommandContext cmdContext) {
		return (AlignmentMode) cmdContext.peekCommandMode();
	}

	protected Alignment lookupAlignment(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext.getObjectContext(), Alignment.class, 
				Alignment.pkMap(getAlignmentName()));
	}
	
	
}
