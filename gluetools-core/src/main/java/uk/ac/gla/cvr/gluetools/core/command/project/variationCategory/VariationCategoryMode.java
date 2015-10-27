package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.VariationCategoryCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = VariationCategoryModeCommandFactory.class)
public class VariationCategoryMode extends CommandMode<VariationCategoryCommand> implements InsideProjectMode {

	
	private String vcatName;
	private Project project;
	
	public VariationCategoryMode(Project project, VariationCategoryCommand command, String vcatName) {
		super(command, vcatName);
		this.vcatName = vcatName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(VariationCategoryModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "vcatName", vcatName);
		}
	}

	public String getVcatName() {
		return vcatName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	
}
