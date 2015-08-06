package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.FeatureCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;

@CommandModeClass(commandFactoryClass = FeatureModeCommandFactory.class)
public class FeatureMode extends CommandMode<FeatureCommand> {

	
	private String featureName;
	
	public FeatureMode(CommandContext cmdContext, FeatureCommand command, String featureName) {
		super(command, featureName);
		this.featureName = featureName;
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

	
}
