package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class VariationsCommand extends ModuleProvidedCommand<PreviewVariationsResult, MutationFrequenciesReporter> implements ProvidedProjectModeCommand {

	
	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String REFERENCE_NAME = "referenceName";
	public static final String FEATURE_NAME = "featureName";
	
	private String alignmentName;
	private String referenceName;
	private String featureName;
	private boolean recursive;
	private Optional<Expression> whereClause;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	protected String getAlignmentName() {
		return alignmentName;
	}

	protected String getReferenceName() {
		return referenceName;
	}

	protected String getFeatureName() {
		return featureName;
	}

	protected boolean isRecursive() {
		return recursive;
	}

	protected Optional<Expression> getWhereClause() {
		return whereClause;
	}
	
}
