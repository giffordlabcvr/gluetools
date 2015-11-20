package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.ListVariationCommand.ListVariationResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.positionVariation.PositionVariation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;


@CommandClass( 
		commandWords={"list", "variation"},
		docoptUsages={"[-t <type> -s <refStart> -e <refEnd>]"},
		docoptOptions={
				"-t <type>, --type <type>              Translation type, NUCLEOTIDE or AMINO_ACID", 
				"-s <refStart>, --refStart <refStart>  Start position",
				"-e <refEnd>, --refEnd <refEnd>        End position",
		},
		description="List feature variations",
		furtherHelp="If <type>, <refStart> and <refEnd> are supplied, these qualify the results accordingly. "+
		"In this case, the returned variations are those which cover the specified region."
	) 
public class ListVariationCommand extends FeatureLocModeCommand<ListVariationResult> {
	

	public static final String VARIATION_CATEGORIES = "variationCategories";

	public static final String TYPE = "type";
	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	
	private TranslationFormat translationFormat;
	private Integer refStart;
	private Integer refEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		translationFormat = PluginUtils.configureEnumProperty(TranslationFormat.class, configElem, TYPE, false);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, false);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, false);
		if(! ( (translationFormat != null && refStart != null && refEnd != null) || 
			   (translationFormat == null && refStart == null && refEnd == null) ) ) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <type>, <refStart> and <refEnd> must be specified, or none of these");
	}

	@Override
	public ListVariationResult execute(CommandContext cmdContext) {
		List<Variation> variations;
		if(translationFormat == null) {
			SelectQuery query = new SelectQuery(Variation.class, 
					ExpressionFactory
						.matchExp(Variation.FEATURE_NAME_PATH, getFeatureName())
						.andExp(ExpressionFactory
								.matchExp(Variation.REF_SEQ_NAME_PATH, getRefSeqName())
					));
			query.addOrdering(new Ordering(Variation.TRANSCRIPTION_TYPE_PROPERTY, SortOrder.ASCENDING));
			query.addOrdering(new Ordering(Variation.REF_START_PROPERTY, SortOrder.ASCENDING));
			query.addOrdering(new Ordering(Variation.NAME_PROPERTY, SortOrder.ASCENDING));
			variations = GlueDataObject.query(cmdContext, Variation.class, query);
		} else {
			SelectQuery query = new SelectQuery(PositionVariation.class, 
					ExpressionFactory
						.matchExp(PositionVariation.FEATURE_NAME_PATH, getFeatureName())
						.andExp(ExpressionFactory
								.matchExp(PositionVariation.REF_SEQ_NAME_PATH, getRefSeqName()))
						.andExp(ExpressionFactory
								.matchExp(PositionVariation.TRANSLATION_TYPE_PROPERTY, translationFormat.name()))
						.andExp(ExpressionFactory
								.greaterOrEqualExp(PositionVariation.POSITION_PROPERTY, refStart))
						.andExp(ExpressionFactory
								.lessOrEqualExp(PositionVariation.POSITION_PROPERTY, refEnd))
					);
			query.addOrdering(new Ordering(PositionVariation.POSITION_PROPERTY, SortOrder.ASCENDING));
			query.addOrdering(new Ordering(PositionVariation.VARIATION_NAME_PATH, SortOrder.ASCENDING));
			List<PositionVariation> positionVariations = GlueDataObject.query(cmdContext, PositionVariation.class, query);
			variations = positionVariations.stream().map(PositionVariation::getVariation).collect(Collectors.toList());
		}

		return new ListVariationResult(variations);
	}

	
	public static class ListVariationResult extends ListResult {

		protected ListVariationResult(List<Variation> results) {
			super(Variation.class, results, 
					Stream.concat(ListResult.propertyPaths(Variation.class).stream(), 
							Arrays.asList(VARIATION_CATEGORIES).stream()).collect(Collectors.toList()), 
							new HeaderResolver());
		}

		public static class HeaderResolver extends MapResult.DefaultResolveHeaderFunction<Variation> {

			@Override
			public Object apply(Variation variation, String header) {
				if(header.equals(VARIATION_CATEGORIES)) {
					List<String> vcatNames = variation.getVariationCategoryNames();
					if(!vcatNames.isEmpty()) {
						return String.join(", ", vcatNames);
					} else {
						return null;
					}
				} 
				return super.apply(variation, header);
			}
			
		}

	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("type", TranslationFormat.class);
		}
	}


}
