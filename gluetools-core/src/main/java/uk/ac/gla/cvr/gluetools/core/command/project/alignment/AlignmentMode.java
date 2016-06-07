package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.AlignmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.RenderableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = AlignmentModeCommandFactory.class)
public class AlignmentMode extends CommandMode<AlignmentCommand> implements ConfigurableObjectMode, InsideAlignmentMode, RenderableObjectMode {
	
	private String alignmentName;
	private Project project;
	
	public AlignmentMode(Project project, AlignmentCommand command, String alignmentName) {
		super(command, alignmentName);
		this.alignmentName = alignmentName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(AlignmentModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "alignmentName", alignmentName);
		}
	}

	public String getAlignmentName() {
		return alignmentName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public ConfigurableTable getConfigurableTable() {
		return ConfigurableTable.alignment;
	}

	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupAlignment(cmdContext);
	}

	protected Alignment lookupAlignment(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(getAlignmentName()));
	}

	@Override
	public GlueDataObject getRenderableObject(CommandContext cmdContext) {
		return lookupAlignment(cmdContext);
	}


	
}
