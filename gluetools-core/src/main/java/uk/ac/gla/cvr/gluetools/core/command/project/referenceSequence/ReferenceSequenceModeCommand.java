package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class ReferenceSequenceModeCommand<R extends CommandResult> extends Command<R> {


	private String refSeqName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, "refSeqName", true);
	}

	protected String getRefSeqName() {
		return refSeqName;
	}

	protected ReferenceSequence lookupRefSeq(CommandContext cmdContext) {
		ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext.getObjectContext(), 
				ReferenceSequence.class, ReferenceSequence.pkMap(getRefSeqName()), false);
		return refSeq;
	}

	@SuppressWarnings("rawtypes")
	public abstract static class FeatureLocNameCompleter extends AdvancedCmdCompleter {
		
		public FeatureLocNameCompleter() {
			super();
			registerVariableInstantiator("featureName", 
					new QualifiedDataObjectNameInstantiator(FeatureLocation.class, FeatureLocation.FEATURE_NAME_PATH) {
				@Override
				protected void qualifyResults(CommandMode cmdMode,
						Map<String, Object> bindings, Map<String, Object> qualifierValues) {
					String refSeqName = ((ReferenceSequenceMode) cmdMode).getRefSeqName();
					qualifierValues.put(FeatureLocation.REF_SEQ_NAME_PATH, refSeqName);
				}
			});
		}
	}

	
	
	protected static ReferenceSequenceMode getRefSeqMode(CommandContext cmdContext) {
		return (ReferenceSequenceMode) cmdContext.peekCommandMode();
	}
}
