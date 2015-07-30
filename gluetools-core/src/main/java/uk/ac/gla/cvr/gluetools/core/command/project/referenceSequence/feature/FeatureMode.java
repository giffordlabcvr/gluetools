package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.FeatureCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

@CommandModeClass(commandFactoryClass = FeatureModeCommandFactory.class)
public class FeatureMode extends CommandMode<FeatureCommand> {

	
	private String featureName;
	
	public FeatureMode(CommandContext cmdContext, FeatureCommand command, String featureName) {
		super(command, featureName);
		this.featureName = featureName;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(FeatureModeCommand.class.isAssignableFrom(cmdClass)) {
			GlueXmlUtils.appendElementWithText(elem, "featureName", featureName, JsonType.String);
		}
	}

	public String getFeatureName() {
		return featureName;
	}

	
}
