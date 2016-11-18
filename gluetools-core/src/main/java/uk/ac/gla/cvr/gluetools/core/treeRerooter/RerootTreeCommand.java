package uk.ac.gla.cvr.gluetools.core.treeRerooter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.newick.NewickToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeafFinder;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeafLister;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"reroot-tree"}, 
		description = "Reroot a Newick tree using a specific outgroup", 
		docoptUsages = { "-i <inputFile> -g <leafName> -o <outputFile> -r"},
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>     Input file",
				"-g <leafName>, --outgroup <leafName>        Specify outgroup leaf",
				"-o <outputFile>, --outputFile <outputFile>  Output file",
				"-r, --removeOutgroup                        Remove outgroup branch in output",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class RerootTreeCommand extends ModulePluginCommand<OkResult, TreeRerooter> {

	public static final String LEAF_NAME = "leafName";
	public static final String INPUT_FILE = "inputFile";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String REMOVE_OUTGROUP = "removeOutgroup";
	
	private String leafName;
	private String inputFile;
	private String outputFile;
	private Boolean removeOutgroup;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.leafName = PluginUtils.configureStringProperty(configElem, LEAF_NAME, true);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		this.removeOutgroup = PluginUtils.configureBooleanProperty(configElem, REMOVE_OUTGROUP, true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, TreeRerooter treeRerooter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		PhyloTree phyloTree = loadTree(consoleCmdContext, inputFile);
		PhyloLeafFinder phyloLeafFinder = new PhyloLeafFinder(l -> l.getName().equals(leafName));
		phyloTree.accept(phyloLeafFinder);
		PhyloLeaf foundLeaf = phyloLeafFinder.getPhyloLeaf();
		if(foundLeaf == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Leaf "+leafName+" not found in file "+inputFile);
		}
		PhyloBranch leafBranch = foundLeaf.getParentPhyloBranch();
		PhyloTree rerootedTree = treeRerooter.rerootTree(leafBranch, leafBranch.getLength().divide(BigDecimal.valueOf(2.0)));
		if(removeOutgroup) {
			
		}
	}

	private static PhyloTree loadTree(ConsoleCommandContext cmdContext,
			String inputFileName) {
		byte[] treeBytes = cmdContext.loadBytes(inputFileName);
		String treeString = new String(treeBytes);
		PhyloTree phyloTree = new NewickToPhyloTreeParser().parseNewick(treeString);
		return phyloTree;
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("inputFile", false);
			registerVariableInstantiator("leafName", new AdvancedCmdCompleter.VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					try {
						String inputFileName = (String) bindings.get("inputFile");
						PhyloTree phyloTree = loadTree(cmdContext, inputFileName);
						PhyloLeafLister phyloLeafLister = new PhyloLeafLister();
						phyloTree.accept(phyloLeafLister);
						return phyloLeafLister.getPhyloLeaves().stream()
								.map(pl -> new CompletionSuggestion(pl.getName(), true))
								.collect(Collectors.toList());
					} catch(Exception e) {
						return null;
					}
				}
			});
			registerPathLookup("outputFile", false);
		}
		
	}
	
}
