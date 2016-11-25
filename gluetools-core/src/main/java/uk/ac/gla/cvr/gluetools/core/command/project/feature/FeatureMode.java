package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.FeatureCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = FeatureModeCommandFactory.class)
public class FeatureMode extends CommandMode<FeatureCommand> implements ConfigurableObjectMode {

	
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

	@Override
	public String getTableName() {
		return ConfigurableTable.feature.name();
	}

	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupFeature(cmdContext);
	}

	protected Feature lookupFeature(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));
	}

	
}
