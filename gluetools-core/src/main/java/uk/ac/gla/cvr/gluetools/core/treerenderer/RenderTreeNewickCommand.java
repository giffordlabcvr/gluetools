package uk.ac.gla.cvr.gluetools.core.treerenderer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloNewickUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTree;

@CommandClass(
		commandWords={"render-tree", "newick"}, 
		description = "Generate a Newick tree file from a constrained alignment tree", 
		docoptUsages = { "<almtName> [-w <whereClause>] -f <fileName>"},
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>  Qualify members",
				"-f <fileName>, --fileName <fileName>           Output to file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class RenderTreeNewickCommand extends ModulePluginCommand<CommandResult, TreeRenderer>{

	public static final String ALIGNMENT_NAME = "almtName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String FILE_NAME = "fileName";
	
	private String almtName;
	private Optional<Expression> whereClause;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, TreeRenderer treeRenderer) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName));
		alignment.getConstrainingRef(); // check constrained
		PhyloTree phyloTree = 
				treeRenderer.phyloTreeFromAlignment(cmdContext, alignment, whereClause, true, true);
		String newickString = PhyloNewickUtils.phyloTreeToNewick(phyloTree);
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		consoleCmdContext.saveBytes(fileName, newickString.getBytes());
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
			registerPathLookup("fileName", false);
		}
	}
	
}
