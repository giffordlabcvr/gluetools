package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"list", "module"}, 
	docoptUsages={"[-t <moduleType>]"},
	docoptOptions = {
	"-t <moduleType>, --moduleType <moduleType>  List modules of a specific type"},
	description="List modules") 
public class ListModuleCommand extends ProjectModeCommand<ListResult> {

	private String moduleType;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.moduleType = PluginUtils.configureStringProperty(configElem, "moduleType", false);
	}

	@Override
	public ListResult execute(CommandContext cmdContext) {
		List<Module> modules = listModules(cmdContext, moduleType);
		
		return new ListResult(cmdContext, Module.class, modules, Arrays.asList("name", "type"), 
				new BiFunction<Module, String, Object>() {
					@Override
					public Object apply(Module t, String u) {
						if(u.equals("name")) {
							return t.getName();
						} else if(u.equals("type")) {
							return t.getType();
						}
						return null;
					}
		});
	}

	public static List<Module> listModules(CommandContext cmdContext, String moduleType) {
		List<Module> modules = GlueDataObject.query(cmdContext, Module.class, new SelectQuery(Module.class));
		if(moduleType != null) {
			modules = modules.stream().filter(m -> m.getType().equals(moduleType)).collect(Collectors.toList());
		}
		return modules;
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
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
		}
		
	}
	
}
