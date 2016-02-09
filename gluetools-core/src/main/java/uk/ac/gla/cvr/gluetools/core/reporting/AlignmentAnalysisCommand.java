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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.AlignmentAnalysisCommand.AlignmentAnalysisResult;


@CommandClass(
		commandWords={"alignment", "analysis"}, 
		description = "Show mutations for members of an alignment", 
		docoptUsages = { "<alignmentName> [-r] [-X] [-w <whereClause>] [-c <vcatName>] <referenceName> <featureName>" }, 
		docoptOptions = {
				"-X, --excludeX                                 Exclude mutations to unknown amino acid",
				"-c, --excludeVcat <vcatName>                   Exclude variations in a certain category",
				"-r, --recursive                                Include members of descendent alignments",
				"-w <whereClause>, --whereClause <whereClause>  Qualify included members"},
		furtherHelp = "The alignment named by <alignmentName> must be constrained to a reference. "+
		"The reference sequence named <referenceName> may be the alignment's constraining reference, or "+
		"the constraining reference of any ancestor alignment. The named reference sequence must have a "+
		"location defined for the feature named by <featureName>, this must be a non-informational feature. "+
		"If --recursive is used, the set of alignment members will be expanded to include members of the named "+
		"alignment's child alignments, and those of their child aligments etc. This can therefore be used to analyse "+
		"across a whole evolutionary clade. "+
		"If --excludeX is used, mutations to an unknown amino acid (X) will be omitted from the results. "+
		"By default such mutations are included. "+
		"If a <whereClause> is supplied, this can qualify further the set of "+
		"included alignment members. Example: \n"+
		"  alignment analysis AL_3 -w \"sequence.source.name = 'ncbi-curated'\" MREF NS3",
		metaTags = {}	
)
public class AlignmentAnalysisCommand 
	extends ModulePluginCommand<AlignmentAnalysisResult, MutationFrequenciesReporter> implements ProvidedProjectModeCommand {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String EXCLUDE_X = "excludeX";
	public static final String EXCLUDE_VCAT = "excludeVcat";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String REFERENCE_NAME = "referenceName";
	public static final String FEATURE_NAME = "featureName";
	
	private String alignmentName;
	private String referenceName;
	private String featureName;
	private boolean recursive;
	private boolean excludeX;
	private Optional<Expression> whereClause;
	private String excludeVcatName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		excludeX = PluginUtils.configureBooleanProperty(configElem, EXCLUDE_X, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		excludeVcatName = PluginUtils.configureStringProperty(configElem, EXCLUDE_VCAT, false);
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		
	}



	@Override
	protected AlignmentAnalysisResult execute(CommandContext cmdContext, MutationFrequenciesReporter modulePlugin) {
		return modulePlugin.doSingleAlignmentAnalysis(cmdContext, alignmentName, recursive, whereClause, excludeVcatName, 
				referenceName, featureName, null, excludeX);
	}

	
	


	public static class AlignmentAnalysisResult extends TableResult {

		public static final String REF_AMINO_ACID = "refAA"; // omit?
		public static final String CODON = "codon";
		public static final String MUT_AMINO_ACID = "mutAA";
		public static final String TOTAL_MEMBERS = "totalMembers";
		public static final String MUTATION_MEMBERS = "mutationMembers";
		
		
		public AlignmentAnalysisResult(List<Map<String, Object>> rowData) {
			super("singleAlignmentAnalysisResult", 
					Arrays.asList(REF_AMINO_ACID, CODON, MUT_AMINO_ACID, MUTATION_MEMBERS, TOTAL_MEMBERS),
					rowData);
		}
		
	}

	@CompleterClass
	public static class Completer extends AlignmentAnalysisCommandCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("vcatName", VariationCategory.class, VariationCategory.NAME_PROPERTY);
		}
	}
	
}
