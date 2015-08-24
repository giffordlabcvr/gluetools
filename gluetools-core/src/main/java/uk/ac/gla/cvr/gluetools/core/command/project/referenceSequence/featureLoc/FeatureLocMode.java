package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.FeatureLocCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;

@CommandModeClass(commandFactoryClass = FeatureLocModeCommandFactory.class)
public class FeatureLocMode extends CommandMode<FeatureLocCommand> {

	
	private String featureName;
	
	public FeatureLocMode(CommandContext cmdContext, FeatureLocCommand command, String featureName) {
		super(command, featureName);
		this.featureName = featureName;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(FeatureLocModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "featureName", featureName);
		}
	}

	public String getFeatureName() {
		return featureName;
	}

	
}
