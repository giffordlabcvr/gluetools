package uk.ac.gla.cvr.gluetools.core.curation.phylogeny;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.VariableInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.NewickPhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTree;

public abstract class GenerateNucleotidePhylogenyCommand<P extends PhylogenyGenerator<P>> extends ModulePluginCommand<OkResult, P> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String INCLUDE_ALL_COLUMNS = "includeAllColumns";
	public static final String MIN_COLUMN_USAGE = "minColUsage";

	public static final String FILE_NAME = "fileName";
	public static final String FIELD_NAME = "fieldName";
	
	private String alignmentName;
	private String acRefName;
	private String featureName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private Boolean includeAllColumns;
	private Integer minColUsage;

	private String fileName;
	private String fieldName;
	

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, false);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, false);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		includeAllColumns = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, INCLUDE_ALL_COLUMNS, false)).orElse(false);
		minColUsage = PluginUtils.configureIntProperty(configElem, MIN_COLUMN_USAGE, false);

		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, false);

		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if(acRefName != null && featureName == null || acRefName == null && featureName != null) {
			usageError2();
		}
		if(fileName == null && fieldName == null || fileName != null && fieldName != null) {
			usageError3();
		}
		if(this.minColUsage != null && !this.includeAllColumns) {
			usageError4();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}
	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <acRefName> and <featureName> must be specified or neither");
	}
	private void usageError3() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or <fieldName> must be specified, but not both");
	}
	private void usageError4() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <minColUsage> argument may only be used if <includeAllColumns> is specified");
	}
	

	@Override
	protected final OkResult execute(CommandContext cmdContext, P modulePlugin) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		if(fieldName != null) {
			project.checkProperty(ConfigurableTable.alignment.name(), fieldName, FieldType.VARCHAR, true);
		}
		
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, true, whereClause);
		Map<Map<String, String>, DNASequence> memberNucleotideAlignment = FastaAlignmentExporter.exportAlignment(cmdContext, acRefName, featureName, includeAllColumns, minColUsage, 
				null, alignment, almtMembers);
		
		PhyloTree phyloTree = generatePhylogeny(cmdContext, modulePlugin, memberNucleotideAlignment);
		
		NewickPhyloTreeVisitor newickPhyloTreeVisitor = new NewickPhyloTreeVisitor();
		phyloTree.accept(newickPhyloTreeVisitor);
		String newickString = newickPhyloTreeVisitor.getNewickString();
		
		if(fileName != null) {
			// save newick string to file.
			ConsoleCommandContext consoleCmdContext = ((ConsoleCommandContext) cmdContext);
			consoleCmdContext.saveBytes(fileName, newickString.getBytes());
		} else {
			// save newick string to field.
			PropertyCommandDelegate.executeSetField(cmdContext, project, ConfigurableTable.alignment.name(), 
					alignment, fieldName, newickString, false);
		}
		
		return new OkResult();
	}

	protected abstract PhyloTree generatePhylogeny(CommandContext cmdContext, P modulePlugin, Map<Map<String, String>, DNASequence> memberNucleotideAlignment);


	public static class PhylogenyCommandCompleter extends FastaAlignmentExportCommandDelegate.ExportCompleter {

		public PhylogenyCommandCompleter() {
			super();
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
					List<String> modifiableFieldNames = project.getModifiableFieldNames(ConfigurableTable.alignment.name());
					return modifiableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
				}
			});
		}

	}

	
}
