package uk.ac.gla.cvr.gluetools.core.phylogenyImporter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylogenyImporter.PhylogenyImporter.AlignmentPhylogeny;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"import", "phylogeny"}, 
		description = "Import a phylogenetic file from a tree", 
		docoptUsages={"<alignmentName> [-c] (-w <whereClause> | -a) -i <inputFile> <inputFormat> (-f <fieldName> | -p)"},
		docoptOptions={
			"-c, --recursive                                Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		    "-a, --allMembers                               All members",
			"-i <inputFile>, --inputFile <inputFile>        Phylogeny input file",
			"-f <fieldName>, --fieldName <fieldName>        Phylogeny field name",
			"-p, --preview                                  Preview only"},
		metaTags = {CmdMeta.consoleOnly}, 
		furtherHelp = "Imports a phylogenetic tree from a Newick file, and breaks it up in order to annotate \n"+
		"the alignment tree, by populating field <fieldName> of alignment objects. \n"+
		"The alignment members selected by the <alignmentName>, --recursive and <whereClause>/--allMembers options \n"+
		"must exactly match the leaf nodes of the imported tree. \n"+
		"The gross structure of the imported tree must match the structure of the alignment tree."
)
public class ImportPhylogenyCommand extends ModulePluginCommand<ImportPhylogenyResult, PhylogenyImporter>{

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";

	public static final String INPUT_FILE = "inputFile";
	public static final String INPUT_FORMAT = "inputFormat";
	public static final String FIELD_NAME = "fieldName";
	public static final String PREVIEW = "preview";
	
	private String alignmentName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Boolean allMembers;

	private String inputFile;
	private PhyloFormat inputFormat;
	private String fieldName;
	private Boolean preview;
	

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);

		inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
		inputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, INPUT_FORMAT, true);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, false);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, false);

		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if((fieldName != null && preview != null && preview) || (fieldName == null && (preview == null || !preview)) ) {
			usageError2();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or --allMembers must be specified, but not both");
	}

	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fieldName> or --preview must be specified, but not both");
	}

	@Override
	protected ImportPhylogenyResult execute(CommandContext cmdContext, PhylogenyImporter phylogenyImporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		
		PhyloTree phyloTree = inputFormat.parse(consoleCmdContext.loadBytes(inputFile));
		List<AlignmentPhylogeny> almtPhylogenies = 
				phylogenyImporter.previewImportPhylogeny(cmdContext, phyloTree, alignmentName, recursive, whereClause);
		if(fieldName != null) {
			for(AlignmentPhylogeny almtPhylogeny: almtPhylogenies) {
				// save string to field in format based on project setting.
				PropertyCommandDelegate.executeSetField(cmdContext, project, ConfigurableTable.alignment.name(), 
						almtPhylogeny.getAlignment(), fieldName, 
						new String(Alignment.getPhylogenyPhyloFormat(cmdContext).generate(almtPhylogeny.getPhyloTree())), false);
				
			}
		}
		return new ImportPhylogenyResult(almtPhylogenies);
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerPathLookup("inputFile", false);
			registerEnumLookup("inputFormat", PhyloFormat.class);
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
