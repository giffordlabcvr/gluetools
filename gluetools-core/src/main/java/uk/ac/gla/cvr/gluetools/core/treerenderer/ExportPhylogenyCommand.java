package uk.ac.gla.cvr.gluetools.core.treerenderer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"export", "phylogeny"}, 
		description = "Export a phylogeny from a constrained alignment tree", 
		docoptUsages = { "<almtName> -f <fieldName> [-c] -o <outputFile> <outputFormat>"},
		docoptOptions = { 
				"-c, --recursive                             Include child alignments",
				"-f <fieldName>, --fieldName <fieldName>     Alignment phylogeny field",
				"-o <outputFile>, --outputFile <outputFile>  Output to file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class ExportPhylogenyCommand extends ModulePluginCommand<CommandResult, PhyloExporter>{

	public static final String ALIGNMENT_NAME = "almtName";
	public static final String FIELD_NAME = "fieldName";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String OUTPUT_FORMAT = "outputFormat";
	public static final String RECURSIVE = "recursive";
	
	private String almtName;
	private String fieldName;
	private String outputFile;
	private PhyloFormat outputFormat;
	private Boolean recursive;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		this.outputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, OUTPUT_FORMAT, true);
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, PhyloExporter phyloExporter) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName));
		alignment.getConstrainingRef(); // check constrained
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		project.checkProperty(ConfigurableTable.alignment.name(), fieldName, FieldType.VARCHAR, false);
		PhyloTree phyloTree = 
				PhyloExporter.exportAlignmentPhyloTree(cmdContext, alignment, fieldName, recursive);
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		consoleCmdContext.saveBytes(outputFile, outputFormat.generate(phyloTree));
		return new OkResult();
		
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			registerVariableInstantiator("almtName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					return GlueDataObject.query(cmdContext, Alignment.class, new SelectQuery(Alignment.class))
							.stream()
							.filter(almt -> almt.isConstrained())
							.map(almt -> new CompletionSuggestion(almt.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerPathLookup("outputFile", false);
			registerEnumLookup("outputFormat", PhyloFormat.class);
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
