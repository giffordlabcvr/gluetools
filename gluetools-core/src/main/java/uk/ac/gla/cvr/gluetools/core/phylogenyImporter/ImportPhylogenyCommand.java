package uk.ac.gla.cvr.gluetools.core.phylogenyImporter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.callback.ConfirmationCallback;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

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
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.NewickToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTreeVisitor;

@CommandClass(
		commandWords={"import", "phylogeny"}, 
		description = "Import a phylogenetic file from a tree", 
		docoptUsages={"<alignmentName> [-c] (-w <whereClause> | -a) -i <fileName> -p <fieldName>"},
		docoptOptions={
			"-c, --recursive                                Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		    "-a, --allMembers                               All members",
			"-i <fileName>, --fileName <fileName>           Phylogeny input file",
			"-p <fieldName>, --fieldName <fieldName>        Phylogeny field name"},
		metaTags = {CmdMeta.consoleOnly}, 
		furtherHelp = "Imports a phylogenetic tree from a Newick file, and breaks it up in order to annotate \n"+
		"the alignment tree, by populating field <fieldName> of alignment objects. \n"+
		"The alignment members elected by the <alignmentName>, --recursive and <whereClause>/--allMembers options \n"+
		"must exactly match the leaf nodes of the imported tree."
)
public class ImportPhylogenyCommand extends ModulePluginCommand<ImportPhylogenyResult, PhylogenyImporter>{

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";

	public static final String FILE_NAME = "fileName";
	public static final String FIELD_NAME = "fieldName";
	
	private String alignmentName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Boolean allMembers;

	private String fileName;
	private String fieldName;
	

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);

		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);

		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}

	@Override
	protected ImportPhylogenyResult execute(CommandContext cmdContext, PhylogenyImporter phylogenyImporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		Project project = ((InsideProjectMode) consoleCmdContext.peekCommandMode()).getProject();
		
		byte[] phylogenyBytes = consoleCmdContext.loadBytes(fileName);
		String phylogenyString = new String(phylogenyBytes);
		NewickToPhyloTreeParser newickToPhyloTreeParser = new NewickToPhyloTreeParser();
		PhyloTree phyloTree = newickToPhyloTreeParser.parseNewick(phylogenyString);
		
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = 
				AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, false, whereClause);

		Set<Map<String,String>> almtMemberPkMaps = almtMembers.stream().map(m -> m.pkMap()).collect(Collectors.toSet());
		Set<Map<String,String>> leafNodePkMaps = new LinkedHashSet<Map<String,String>>();

		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafNodeName = phyloLeaf.getName();
				Map<String, String> leafNodePkMap = project.targetPathToPkMap(ConfigurableTable.alignment_member.name(), leafNodeName);
				leafNodePkMaps.add(leafNodePkMap);
			}
		});
		
		if(!leafNodePkMaps.equals(almtMemberPkMaps)) {
			Set<Map<String,String>> inMemberListButNotFile = new LinkedHashSet<Map<String,String>>();
			inMemberListButNotFile.addAll(almtMemberPkMaps);
			inMemberListButNotFile.removeAll(leafNodePkMaps);
			Set<Map<String,String>> inFileButNotMemberList = new LinkedHashSet<Map<String,String>>();
			inFileButNotMemberList.addAll(leafNodePkMaps);
			inFileButNotMemberList.removeAll(almtMemberPkMaps);
			GlueLogger.getGlueLogger().fine(inMemberListButNotFile.size()+" members in selected set but not in file.");
			GlueLogger.getGlueLogger().fine(inFileButNotMemberList.size()+" leaf nodes in file but not in selected set.");
			throw new ImportPhylogenyException(ImportPhylogenyException.Code.MEMBER_LEAF_MISMATCH, fileName);
		}
		
		ImportPhylogenyResult result = new ImportPhylogenyResult();
		return result;
	}

	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerPathLookup("fileName", false);
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
