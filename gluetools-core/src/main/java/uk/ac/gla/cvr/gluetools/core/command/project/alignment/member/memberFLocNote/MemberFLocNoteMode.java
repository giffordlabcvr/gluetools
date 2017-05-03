package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.memberFLocNote;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberFLocNoteCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = MemberFLocNoteModeCommandFactory.class)
public class MemberFLocNoteMode extends CommandMode<MemberFLocNoteCommand> implements ConfigurableObjectMode {
	
	private String alignmentName;
	private String sourceName;
	private String sequenceID;
	private String refSeqName;
	private String featureName;
	private Project project;
	
	public MemberFLocNoteMode(Project project, MemberFLocNoteCommand command, 
			String alignmentName, 
			String sourceName, 
			String sequenceID, 
			String refSeqName,
			String featureName) {
		super(command, refSeqName, featureName);
		this.alignmentName = alignmentName;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
		this.refSeqName = refSeqName;
		this.featureName = featureName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(MemberFLocNoteModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "refSeqName", refSeqName);
			appendModeConfigToElem(elem, "featureName", featureName);
		}
	}

	public String getAlignmentName() {
		return alignmentName;
	}
	
	public String getSourceName() {
		return sourceName;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public String getRefSeqName() {
		return refSeqName;
	}

	public String getFeatureName() {
		return featureName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public String getTableName() {
		return ConfigurableTable.member_floc_note.name();
	}

	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupMemberFLocNote(cmdContext);
	}

	protected MemberFLocNote lookupMemberFLocNote(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, MemberFLocNote.class, 
				MemberFLocNote.pkMap(getAlignmentName(), getSourceName(), getSequenceID(),
						getRefSeqName(), getFeatureName()));
	}


	
}
