package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.varAlmtNote;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class VarAlmtNoteModeCommand<R extends CommandResult> extends VariationModeCommand<R> {

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

	protected static VarAlmtNoteMode getAlmtNoteMode(CommandContext cmdContext) {
		return (VarAlmtNoteMode) cmdContext.peekCommandMode();
	}

	protected VarAlmtNote lookupVarAlmtNote(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, VarAlmtNote.class, 
				VarAlmtNote.pkMap(getAlignmentName(), getRefSeqName(), getFeatureName(), getVariationName()));
	}
	
	
}
