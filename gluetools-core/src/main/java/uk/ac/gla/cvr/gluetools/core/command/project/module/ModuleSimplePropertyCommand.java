package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ModuleSimplePropertyCommand<R extends CommandResult> extends ModuleDocumentCommand<R> {

	public static final String PROPERTY_NAME = "propertyName";
	
	private String propertyName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.propertyName = PluginUtils.configureStringProperty(configElem, PROPERTY_NAME, true);
	}

	protected void checkPropertyName(CommandContext cmdContext, Module module) {
		if(!module.getModulePlugin(cmdContext.getGluetoolsEngine(), false).getSimplePropertyNames().contains(getPropertyName())) {
			throw new ModuleException(ModuleException.Code.NO_SUCH_MODULE_PROPERTY, getPropertyName());
		}
	}

	protected String getPropertyName() {
		return propertyName;
	}
	
	@SuppressWarnings("rawtypes")
	public static class PropertyNameCompleter extends AdvancedCmdCompleter {
		public PropertyNameCompleter() {
			super();
			registerVariableInstantiator("propertyName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					String moduleName = ((ModuleMode) cmdContext.peekCommandMode()).getModuleName();
					Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
					return module.getModulePlugin(cmdContext.getGluetoolsEngine(), false).getSimplePropertyNames()
							.stream()
							.map(pn -> new CompletionSuggestion(pn, true))
							.collect(Collectors.toList());
				}
			});
		}

	}


	
}
