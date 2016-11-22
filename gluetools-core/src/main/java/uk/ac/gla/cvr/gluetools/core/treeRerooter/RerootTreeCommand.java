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
import uk.ac.gla.cvr.gluetools.core.newick.NewickPhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.newick.NewickToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeafFinder;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeafLister;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeMidpointFinder;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeMidpointResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"reroot-tree"}, 
		description = "Reroot a Newick tree", 
		docoptUsages = { "-i <inputFile> (-g <outgroup> [-r] | -m) -o <outputFile> "},
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>     Input file",
				"-g <outgroup>, --outgroup <outgroup>        Specify outgroup leaf",
				"-r, --removeOutgroup                        Remove outgroup branch in output",
				"-m, --midpoint                              Use midpoint rooting",
				"-o <outputFile>, --outputFile <outputFile>  Output file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class RerootTreeCommand extends ModulePluginCommand<OkResult, TreeRerooter> {

	public static final String OUTGROUP = "outgroup";
	public static final String INPUT_FILE = "inputFile";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String REMOVE_OUTGROUP = "removeOutgroup";
	public static final String MIDPOINT = "midpoint";
	
	private String outgroup;
	private String inputFile;
	private String outputFile;
	private Boolean removeOutgroup;
	private Boolean midpoint;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
		this.outgroup = PluginUtils.configureStringProperty(configElem, OUTGROUP, false);
		this.removeOutgroup = PluginUtils.configureBooleanProperty(configElem, REMOVE_OUTGROUP, false);
		this.midpoint = PluginUtils.configureBooleanProperty(configElem, MIDPOINT, false);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		if( (outgroup == null && (midpoint == null || !midpoint)) ||
				(outgroup != null && (midpoint != null && midpoint))) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <outgroup> or --midpoint must be specified, but not both");
		}
		if(outgroup == null && (removeOutgroup != null && removeOutgroup)) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "The --removeOutgroup option may only be used if <outgroup> is specified");
		}
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, TreeRerooter treeRerooter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		PhyloTree phyloTree = loadTree(consoleCmdContext, inputFile);
		PhyloBranch rerootBranch = null;
		BigDecimal rerootDistance = null;
		if(outgroup != null) {
			PhyloLeafFinder phyloLeafFinder = new PhyloLeafFinder(l -> l.getName().equals(outgroup));
			phyloTree.accept(phyloLeafFinder);
			PhyloLeaf foundLeaf = phyloLeafFinder.getPhyloLeaf();
			if(foundLeaf == null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Leaf "+outgroup+" not found in file "+inputFile);
			}
			rerootBranch = foundLeaf.getParentPhyloBranch();
			rerootDistance = rerootBranch.getLength().divide(BigDecimal.valueOf(2.0));
		} else {
			// midpoint rooting
			PhyloTreeMidpointFinder midpointFinder = new PhyloTreeMidpointFinder();
			PhyloTreeMidpointResult midPointResult = midpointFinder.findMidPoint(phyloTree);
			rerootBranch = midPointResult.getBranch();
			rerootDistance = midPointResult.getRootDistance();
		}
		PhyloTree rerootedTree = treeRerooter.rerootTree(rerootBranch, rerootDistance);
		if(outgroup != null && removeOutgroup) {
			PhyloObject<?> root = rerootedTree.getRoot();
			if(!(root instanceof PhyloInternal)) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Single leaf node in rerooted tree: cannot remove outgroup.");
			}
			PhyloInternal rootPhyloInternal = (PhyloInternal) root;
			List<PhyloBranch> rootBranches = rootPhyloInternal.getBranches();
			if(rootBranches.size() != 2) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unexpected number of branches ("+rootBranches.size()+") at root: cannot remove outgroup.");
			}
			Integer indexOfOutgroup = null;
			for(int i = 0; i < rootBranches.size(); i++) {
				PhyloBranch branch = rootBranches.get(i);
				PhyloSubtree<?> branchSubtree = branch.getSubtree();
				if(branchSubtree instanceof PhyloLeaf && ((PhyloLeaf) branchSubtree).getName().equals(outgroup)) {
					indexOfOutgroup = branch.getChildBranchIndex();
					break;
				}
			}
			if(indexOfOutgroup == null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Outgroup not found at root: cannot remove outgroup.");
			}
			Integer indexOfRemainingTree = 0;
			if(indexOfOutgroup.intValue() == 0) {
				indexOfRemainingTree = 1;
			}
			PhyloBranch remainingTreeBranch = rootBranches.get(indexOfRemainingTree);
			rootPhyloInternal.removeBranch(remainingTreeBranch);
			PhyloSubtree<?> remainingSubtree = remainingTreeBranch.getSubtree();
			rerootedTree.setRoot(remainingSubtree);
		}
		NewickPhyloTreeVisitor newickPhyloTreeVisitor = new NewickPhyloTreeVisitor();
		rerootedTree.accept(newickPhyloTreeVisitor);
		consoleCmdContext.saveBytes(outputFile, newickPhyloTreeVisitor.getNewickString().getBytes());
		return new OkResult();
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
			registerVariableInstantiator("outgroup", new AdvancedCmdCompleter.VariableInstantiator() {
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
