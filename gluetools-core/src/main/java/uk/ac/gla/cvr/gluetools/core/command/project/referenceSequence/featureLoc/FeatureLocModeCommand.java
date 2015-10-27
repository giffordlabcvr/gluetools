package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceSequenceModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class FeatureLocModeCommand<R extends CommandResult> extends ReferenceSequenceModeCommand<R> {


	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}

	protected String getFeatureName() {
		return featureName;
	}
	
	protected static FeatureLocMode getFeatureLocMode(CommandContext cmdContext) {
		return (FeatureLocMode) cmdContext.peekCommandMode();
	}
	
	public FeatureLocation lookupFeatureLoc(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
				FeatureLocation.pkMap(getRefSeqName(), getFeatureName()));
	}

	@SuppressWarnings("rawtypes")
	public abstract static class VariationNameCompleter extends AdvancedCmdCompleter {
		
		public VariationNameCompleter() {
			super();
			registerVariableInstantiator("variationName", new QualifiedDataObjectNameInstantiator(
					Variation.class, Variation.NAME_PROPERTY) {
				@Override
				protected void qualifyResults(CommandMode cmdMode,
						Map<String, Object> bindings, Map<String, Object> qualifierValues) {
					FeatureLocMode featureLocMode = (FeatureLocMode) cmdMode;
					qualifierValues.put(Variation.REF_SEQ_NAME_PATH, featureLocMode.getRefSeqName());
					qualifierValues.put(Variation.FEATURE_NAME_PATH, featureLocMode.getFeatureName());
				}
			});
		}

	}


}
