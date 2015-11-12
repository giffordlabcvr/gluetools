package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocModeCommand.VariationNameCompleter;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"clear", "variation"}, 
	docoptUsages={"[-f <featureName>] [-i <vcatName>]"},
	docoptOptions={
			"-f <featureName>, --feature <featureName>  Delete from the named feature-location",
			"-i <vcatName>, --includeVcat <vcatName>    Delete variations in named category"
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a set of variations") 
public class ClearVariationCommand extends ReferenceSequenceModeCommand<DeleteResult> {

	private String featureName;
	private String includeVcatName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "feature", false);
		includeVcatName = PluginUtils.configureStringProperty(configElem, "includeVcat", false);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Variation.REF_SEQ_NAME_PATH, getRefSeqName());
		
		if(featureName != null) {
			// check feature exists
			GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
			exp = exp.andExp(ExpressionFactory.matchExp(Variation.FEATURE_NAME_PATH, featureName));
		}
		List<Variation> variations = GlueDataObject.query(cmdContext, Variation.class, new SelectQuery(Variation.class, exp));
		
		if(includeVcatName != null) {
			// check variation category exists
			GlueDataObject.lookup(cmdContext, VariationCategory.class, VariationCategory.pkMap(includeVcatName));
			// filter down to the set of variations that have membership of the named category.
			variations = variations.stream()
					.filter(var ->	var.getVcatMemberships().stream()
									.map(vcm -> vcm.getCategory().getName())
									.anyMatch(catName -> catName.equals(includeVcatName)))
					.collect(Collectors.toList());
			
		}

		int numDeleted = 0;
		for(Variation variation: variations) {
			DeleteResult result = 
					GlueDataObject.delete(cmdContext, Variation.class, variation.pkMap(), false);
			numDeleted += result.getNumber();
			
		}
		cmdContext.commit();
		return new DeleteResult(Variation.class, numDeleted);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerVariableInstantiator("featureName", new AdvancedCmdCompleter.VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String refSeqName = ((ReferenceSequenceMode) cmdContext.peekCommandMode()).getRefSeqName();
					ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName));
					return refSequence.getFeatureLocations().stream()
							.map(fl -> new CompletionSuggestion(fl.getFeature().getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerDataObjectNameLookup("vcatName", VariationCategory.class, VariationCategory.NAME_PROPERTY);
		}
		
	}

}
