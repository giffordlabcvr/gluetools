package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
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
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"create", "module"}, 
		docoptUsages={"(-t <moduleType> | -f <fileName> [-r] ) <moduleName>"},
		description="Create a module",
		docoptOptions={
				"-t <moduleType>, --moduleType <moduleType>  Default config for a specific module type", 
				"-f <fileName>, --fileName <fileName>        Config from an XML file",
				"-r, --loadResources                         Also load dependent resources"},
		furtherHelp="Creates a module of the specified type, with the default configuration for that module type.",
		metaTags={ CmdMeta.updatesDatabase, CmdMeta.consoleOnly } ) 
	
public class CreateModuleCommand extends Command<CreateResult> {

	public static final String MODULE_TYPE = "moduleType";
	public static final String FILE_NAME = "fileName";
	public static final String MODULE_NAME = "moduleName";
	private static final String LOAD_RESOURCES = "loadResources";

	private String moduleType;
	private String moduleName;
	private String fileName;
	private boolean loadResources;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.moduleType = PluginUtils.configureStringProperty(configElem, MODULE_TYPE, false);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		this.moduleName = PluginUtils.configureStringProperty(configElem, MODULE_NAME, true);
		this.loadResources = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, LOAD_RESOURCES, false)).orElse(false);
		if(moduleType == null && fileName == null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <moduleType> or <fileName> must be specified.");
		}
		if(fileName == null && loadResources) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "The --loadResources option may only be used if <fileName> is specified.");
		}
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		byte[] config = null;
		
		Module module = GlueDataObject.create(cmdContext, Module.class, Module.pkMap(moduleName), false);
		if(moduleType != null) {
			ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
			if(!pluginFactory.getElementNames().contains(moduleType)) {
				throw new ModuleException(ModuleException.Code.NO_SUCH_MODULE_TYPE, moduleType);
			}
			
			// create document with just a single type element in it.
			Document document = GlueXmlUtils.newDocument();
			document.appendChild(document.createElement(moduleType));
			config = GlueXmlUtils.prettyPrint(document);
			module.setConfig(config);
		} else {
			ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
			module.loadConfig(consoleCmdContext, fileName, loadResources);
		}
		cmdContext.commit();
		return new CreateResult(Module.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("moduleType", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
					return pluginFactory.getElementNames().stream()
							.map(eName -> new CompletionSuggestion(eName, true))
							.collect(Collectors.toList());
				}
			});
			registerPathLookup("fileName", false);
		}
		
	}
	
}
