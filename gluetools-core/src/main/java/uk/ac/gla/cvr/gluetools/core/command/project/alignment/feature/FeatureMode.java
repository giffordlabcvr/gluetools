package uk.ac.gla.cvr.gluetools.core.command.project.alignment.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class FeatureMode extends CommandMode {

	
	private String featureName;
	
	public FeatureMode(CommandContext cmdContext, String featureName) {
		super("feat-"+featureName, new FeatureModeCommandFactory());
		this.featureName = featureName;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(FeatureModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "featureName", featureName);
		}
	}

	public String getFeatureName() {
		return featureName;
	}

	
}
