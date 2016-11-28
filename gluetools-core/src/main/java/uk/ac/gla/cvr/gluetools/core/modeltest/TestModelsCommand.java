package uk.ac.gla.cvr.gluetools.core.modeltest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate.OrderStrategy;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"test", "models"}, 
		description = "Run JModelTest on a nucleotide alignment to compare substitution models", 
		docoptUsages={"<alignmentName> [-r <relRefName> -f <featureName>] [-c] (-w <whereClause> | -a) [-i [-m <minColUsage>]] [-d <dataDir>]"},
		docoptOptions={
				"-r <relRefName>, --relRefName <relRefName>     Related reference",
				"-f <featureName>, --featureName <featureName>  Restrict to a given feature",
				"-c, --recursive                                Include descendent members",
				"-w <whereClause>, --whereClause <whereClause>  Qualify members",
				"-a, --allMembers                               All members",
				"-i, --includeAllColumns                        Include columns for all NTs",
				"-m <minColUsage>, --minColUsage <minColUsage>  Minimum included column usage",
				"-d <dataDir>, --dataDir <dataDir>              Save algorithmic data in this directory"},
		metaTags = { CmdMeta.consoleOnly },
		furtherHelp="If supplied, <dataDir> must either not exist or be an empty directory.")
public class TestModelsCommand extends ModulePluginCommand<TestModelsResult, ModelTester> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String INCLUDE_ALL_COLUMNS = "includeAllColumns";
	public static final String MIN_COLUMN_USAGE = "minColUsage";
	public static final String DATA_DIR = "dataDir";

	private String alignmentName;
	private String relRefName;
	private String featureName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private Boolean includeAllColumns;
	private Integer minColUsage;

	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, false);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, false);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		includeAllColumns = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, INCLUDE_ALL_COLUMNS, false)).orElse(false);
		minColUsage = PluginUtils.configureIntProperty(configElem, MIN_COLUMN_USAGE, false);
		dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);

		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if(relRefName != null && featureName == null || relRefName == null && featureName != null) {
			usageError2();
		}
		if(this.minColUsage != null && !this.includeAllColumns) {
			usageError3();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}
	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <relRefName> and <featureName> must be specified or neither");
	}
	private void usageError3() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <minColUsage> argument may only be used if <includeAllColumns> is specified");
	}
	

	@Override
	protected TestModelsResult execute(CommandContext cmdContext, ModelTester modelTester) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, true, whereClause);
		Map<Map<String, String>, DNASequence> memberNucleotideAlignment = FastaAlignmentExporter.exportAlignment(cmdContext, relRefName, featureName, includeAllColumns, minColUsage, 
				null, alignment, almtMembers);

		TestModelsResult testModelsResult = modelTester.testModels(cmdContext, memberNucleotideAlignment, dataDir);
		return testModelsResult;
	}


	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerEnumLookup("orderStrategy", OrderStrategy.class);
			registerVariableInstantiator("relRefName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String alignmentName = (String) bindings.get("alignmentName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), true);
					if(alignment != null) {
						return(alignment.getRelatedRefs()
								.stream()
								.map(ref -> new CompletionSuggestion(ref.getName(), true)))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String relRefName = (String) bindings.get("relRefName");
					ReferenceSequence acRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relRefName), true);
					if(acRef != null) {
						return(acRef.getFeatureLocations()
								.stream()
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true)))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerPathLookup("dataDir", true);
		}
	}


	
}
