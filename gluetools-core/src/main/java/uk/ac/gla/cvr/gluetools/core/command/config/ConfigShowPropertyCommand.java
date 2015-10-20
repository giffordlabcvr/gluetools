package uk.ac.gla.cvr.gluetools.core.command.config;



import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords = {"config","show"},
		docoptUsages = {"<propertyName>"}, 
		metaTags = { CmdMeta.nonModeWrappable },
		description = "Show the value of a configuration property")
public class ConfigShowPropertyCommand extends Command<ConfigPropertyResult> {

	public static final String PROPERTY_NAME = "propertyName";
	private String propertyName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyName = PluginUtils.configureStringProperty(configElem, PROPERTY_NAME, true);
	}


	@Override
	public ConfigPropertyResult execute(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String propertyValue = propertiesConfiguration.getPropertyValue(propertyName);
		return new ConfigPropertyResult(propertyName, propertyValue);
	}
	
	
	@CompleterClass
	public static class PropertyNameCompleter extends AdvancedCmdCompleter {

		public PropertyNameCompleter() {
			super();
			registerVariableInstantiator("propertyName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getAllPropertyNames()
							.stream().map(pn -> new CompletionSuggestion(pn, true)).collect(Collectors.toList());
				}
			});
		}

	}


}