package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.AlignmentVariationCategoryScanCommand.AlignmentVariationCategoryScanResult;


@CommandClass(
		commandWords={"alignment", "scan", "variation-category"}, 
		description = "Scan alignment members for variations in a specific category", 
		docoptUsages = { "<alignmentName> [-r] [-w <whereClause>] <referenceName> <featureName> <categoryName>" }, 
		docoptOptions = {
				"-r, --recursive                                Include members of descendent alignments",
				"-w <whereClause>, --whereClause <whereClause>  Qualify included members"},
		furtherHelp = "The alignment named by <alignmentName> must be constrained to a reference. "+
		"The reference sequence named <referenceName> may be the alignment's constraining reference, or "+
		"the constraining reference of any ancestor alignment. The named reference sequence must have a "+
		"location defined for the feature named by <featureName>, this must be a non-informational feature. "+
		"The named variation must be defined on this feature location."+
		"If --recursive is used, the set of alignment members will be expanded to include members of the named "+
		"alignment's child alignments, and those of their child aligments etc. This can therefore be used to analyse "+
		"across a whole evolutionary clade. "+
		"If a <whereClause> is supplied, this can qualify further the set of "+
		"included alignment members. Example: \n"+
		"  alignment analysis AL_3 -w \"sequence.source.name = 'ncbi-curated'\" MREF NS3",
		metaTags = {}	
)
public class AlignmentVariationCategoryScanCommand 
	extends ModuleProvidedCommand<AlignmentVariationCategoryScanResult, MutationFrequenciesReporter> implements ProvidedProjectModeCommand {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String REFERENCE_NAME = "referenceName";
	public static final String FEATURE_NAME = "featureName";
	public static final String CATEGORY_NAME = "categoryName";
	
	private String alignmentName;
	private String referenceName;
	private String featureName;
	private boolean recursive;
	private Optional<Expression> whereClause;
	private String categoryName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		categoryName = PluginUtils.configureStringProperty(configElem, CATEGORY_NAME, true);
	}



	@Override
	protected AlignmentVariationCategoryScanResult execute(CommandContext cmdContext, MutationFrequenciesReporter modulePlugin) {
		return modulePlugin.doAlignmentVariationCategoryScan(cmdContext, alignmentName, recursive, whereClause, referenceName, featureName, categoryName);
	}

	
	

	public static class AlignmentVariationCategoryScanResult extends TableResult {

		public static final String ALIGNMENT_NAME = AlignmentMember.ALIGNMENT_NAME_PATH;
		public static final String MEMBER_SOURCE = AlignmentMember.SOURCE_NAME_PATH;
		public static final String MEMBER_SEQUENCE_ID = AlignmentMember.SEQUENCE_ID_PATH;
		public static final String VARIATION_NAME = "variationName";
		public static final String CATEGORY_NAMES = "categoryNames";
		
		public AlignmentVariationCategoryScanResult(List<Map<String, Object>> rowData) {
			super("alignmentVariationCategoryScanResult", 
					Arrays.asList(ALIGNMENT_NAME, MEMBER_SOURCE, MEMBER_SEQUENCE_ID, VARIATION_NAME, CATEGORY_NAMES),
					rowData);
		}
		
	}

	@CompleterClass
	public static class Completer extends AlignmentAnalysisCommandCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("categoryName", VariationCategory.class, VariationCategory.NAME_PROPERTY);
		}
	}
	
}
