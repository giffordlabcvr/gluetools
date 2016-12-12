package uk.ac.gla.cvr.gluetools.core.curation.phylogeny;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class GenerateNucleotidePhylogenyCommand<P extends PhylogenyGenerator<P>> extends ModulePluginCommand<OkResult, P> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String INCLUDE_ALL_COLUMNS = "includeAllColumns";
	public static final String MIN_COLUMN_USAGE = "minColUsage";

	public static final String OUTPUT_FILE = "outputFile";
	public static final String OUTPUT_FORMAT = "outputFormat";
	
	private String alignmentName;
	private String relRefName;
	private String featureName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private Boolean includeAllColumns;
	private Integer minColUsage;

	private String outputFile;
	private PhyloFormat outputFormat;
	
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

		outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		outputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, OUTPUT_FORMAT, true);

		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if(relRefName != null && featureName == null || relRefName == null && featureName != null) {
			usageError2();
		}
		if(this.minColUsage != null && !this.includeAllColumns) {
			usageError4();
		}
		if(this.outputFormat != null && this.outputFile == null) {
			usageError5();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}
	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <relRefName> and <featureName> must be specified or neither");
	}
	private void usageError4() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <minColUsage> argument may only be used if <includeAllColumns> is specified");
	}
	private void usageError5() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <outputFormat> argument may only be used if <outputFile> is specified");
	}
	

	@Override
	protected final OkResult execute(CommandContext cmdContext, P modulePlugin) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		Map<Map<String, String>, DNASequence> memberNucleotideAlignment = FastaAlignmentExporter.exportAlignment(cmdContext, relRefName, featureName, includeAllColumns, minColUsage, 
				null, alignment, almtMembers);
		
		PhyloTree phyloTree = generatePhylogeny(cmdContext, modulePlugin, memberNucleotideAlignment);

		// save bytes to file in specified format.
		ConsoleCommandContext consoleCmdContext = ((ConsoleCommandContext) cmdContext);
		consoleCmdContext.saveBytes(outputFile, outputFormat.generate(phyloTree));

		return new OkResult();
	}

	protected abstract PhyloTree generatePhylogeny(CommandContext cmdContext, P modulePlugin, Map<Map<String, String>, DNASequence> memberNucleotideAlignment);


	public static class PhylogenyCommandCompleter extends FastaAlignmentExportCommandDelegate.ExportCompleter {

		public PhylogenyCommandCompleter() {
			super();
			registerPathLookup("outputFile", false);
			registerEnumLookup("outputFormat", PhyloFormat.class);
		}

	}

	
}
