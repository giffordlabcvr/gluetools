package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.FeatureLocCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = FeatureLocModeCommandFactory.class)
public class FeatureLocMode extends CommandMode<FeatureLocCommand> implements ConfigurableObjectMode, InsideProjectMode {

	private Project project;
	private String refSeqName;
	private String featureName;
	
	public FeatureLocMode(Project project, FeatureLocCommand command, String refSeqName, String featureName) {
		super(command, featureName);
		this.project = project;
		this.refSeqName = refSeqName;		
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

	protected String getFeatureName() {
		return featureName;
	}

	public String getRefSeqName() {
		return refSeqName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public String getTableName() {
		return ConfigurableTable.feature_location.name();
	}

	protected FeatureLocation lookupFeatureLocation(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getRefSeqName(), getFeatureName()));
	}

	
	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupFeatureLocation(cmdContext);
	}

	
}
