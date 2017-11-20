package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"analysis"}, 
		description = "Run analysis from the command line", 
		docoptUsages = { "-i <fileName> [ -v <vCategory> ] ..." },
		docCategory = "Type-specific module commands",
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>     Multi-FASTA file to analyse", 
				"-v <vCategory>, --vCategory <vCategory>  One or more variation categories"},
		metaTags = {CmdMeta.consoleOnly}	
)
public class AnalysisCommand extends ModulePluginCommand<PojoCommandResult<WebAnalysisResult>, WebAnalysisTool> 
	implements ProvidedProjectModeCommand{

	public static final String FILE_NAME = "fileName";
	public static final String VARIATION_CATEGORY = "vCategory";


	private String fileName;
	private List<String> vCategories;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.vCategories = PluginUtils.configureStringsProperty(configElem, VARIATION_CATEGORY);
	}


	@Override
	protected PojoCommandResult<WebAnalysisResult> execute(CommandContext cmdContext, WebAnalysisTool webAnalysisTool) {
		byte[] fileContent = ((ConsoleCommandContext) cmdContext).loadBytes(fileName);
		return new PojoCommandResult<WebAnalysisResult>(webAnalysisTool.analyse(cmdContext, fileContent, vCategories));
	}
	
	@CompleterClass
	public static final class Completer extends ModuleCmdCompleter<WebAnalysisTool> {

		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerVariableInstantiator("vCategory", new ModuleVariableInstantiator() {

				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						WebAnalysisTool webAnalysisTool,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return webAnalysisTool.getVariationCategories()
						.stream()
						.map(vCat -> new CompletionSuggestion(vCat.getName(), true))
						.collect(Collectors.toList());
				}
			});
		}
		
	}
	
}
