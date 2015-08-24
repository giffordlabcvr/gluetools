package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.FeatureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = FeatureModeCommandFactory.class)
public class FeatureMode extends CommandMode<FeatureCommand> implements InsideProjectMode {

	
	private String featureName;
	private Project project;
	
	public FeatureMode(Project project, FeatureCommand command, String featureName) {
		super(command, featureName);
		this.featureName = featureName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(FeatureModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "featureName", featureName);
		}
	}

	public String getFeatureName() {
		return featureName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	
}
