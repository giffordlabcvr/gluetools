package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.varAlmtNote;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationAlmtNoteCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;

@CommandModeClass(commandFactoryClass = VarAlmtNoteModeCommandFactory.class)
public class VarAlmtNoteMode extends CommandMode<VariationAlmtNoteCommand> implements ConfigurableObjectMode {
	
	private String alignmentName;
	private String refSeqName;
	private String featureName;
	private String variationName;
	private Project project;
	
	public VarAlmtNoteMode(Project project, VariationAlmtNoteCommand command, 
			String alignmentName, 
			String refSeqName,
			String featureName, 
			String variationName) {
		super(command, alignmentName);
		this.alignmentName = alignmentName;
		this.refSeqName = refSeqName;
		this.featureName = featureName;
		this.variationName = variationName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(VarAlmtNoteModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "alignmentName", alignmentName);
		}
	}

	public String getAlignmentName() {
		return alignmentName;
	}
	
	public String getRefSeqName() {
		return refSeqName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public String getVariationName() {
		return variationName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public String getTableName() {
		return ConfigurableTable.var_almt_note.name();
	}

	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupVarAlmtNote(cmdContext);
	}

	protected VarAlmtNote lookupVarAlmtNote(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, VarAlmtNote.class, 
				VarAlmtNote.pkMap(getAlignmentName(), getRefSeqName(), getFeatureName(), getVariationName()));
	}


	
}
