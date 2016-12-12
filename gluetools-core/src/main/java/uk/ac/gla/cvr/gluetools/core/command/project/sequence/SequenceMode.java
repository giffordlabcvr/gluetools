package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.RenderableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.SequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

@CommandModeClass(commandFactoryClass = SequenceModeCommandFactory.class)
public class SequenceMode extends CommandMode<SequenceCommand> implements ConfigurableObjectMode, RenderableObjectMode {

	private Project project;
	private String sourceName;
	private String sequenceID;
	
	public SequenceMode(Project project, SequenceCommand command, String sourceName, String sequenceID) {
		super(command, sourceName, sequenceID);
		this.project = project;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(SequenceModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "sourceName", sourceName);
			appendModeConfigToElem(elem, "sequenceID", sequenceID);
		}
	}
	
	public Project getProject() {
		return project;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getSequenceID() {
		return sequenceID;
	}
	
	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupSequence(cmdContext);
	}

	public Sequence lookupSequence(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(getSourceName(), getSequenceID()));
	}

	@Override
	public String getTableName() {
		return ConfigurableTable.sequence.name();
	}

	@Override
	public GlueDataObject getRenderableObject(CommandContext cmdContext) {
		return lookupSequence(cmdContext);
	}

	
}
