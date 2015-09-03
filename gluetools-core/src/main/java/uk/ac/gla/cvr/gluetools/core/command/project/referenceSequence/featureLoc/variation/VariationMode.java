package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.VariationCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = VariationModeCommandFactory.class)
public class VariationMode extends CommandMode<VariationCommand> implements InsideProjectMode {

	
	private String refSeqName;
	private String featureName;
	private String variationName;
	private Project project;
	
	public VariationMode(Project project, VariationCommand command, String refSeqName, String featureName, String variationName) {
		super(command, variationName);
		this.refSeqName = refSeqName;
		this.featureName = featureName;
		this.variationName = variationName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(VariationModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "variationName", variationName);
		}
	}

	public String getVariationName() {
		return variationName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public String getRefSeqName() {
		return refSeqName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	
}
