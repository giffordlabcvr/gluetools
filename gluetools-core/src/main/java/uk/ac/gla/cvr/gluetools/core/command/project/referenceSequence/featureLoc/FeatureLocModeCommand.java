package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
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
		return GlueDataObject.lookup(cmdContext.getObjectContext(), FeatureLocation.class, 
				FeatureLocation.pkMap(getRefSeqName(), getFeatureName()));
	}

	@SuppressWarnings("rawtypes")
	public abstract static class VariationNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				ObjectContext objContext = cmdContext.getObjectContext();
				FeatureLocMode featureLocMode = getFeatureLocMode(cmdContext);
				Expression expression = ExpressionFactory.matchExp(Variation.REF_SEQ_NAME_PATH, featureLocMode.getRefSeqName());
				expression = expression.andExp(ExpressionFactory.matchExp(Variation.FEATURE_NAME_PATH, featureLocMode.getFeatureName()));
				List<Variation> variation = GlueDataObject.query(objContext, Variation.class, 
						new SelectQuery(Variation.class, expression));
				return variation.stream().map(Variation::getName).collect(Collectors.toList());
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}


}
