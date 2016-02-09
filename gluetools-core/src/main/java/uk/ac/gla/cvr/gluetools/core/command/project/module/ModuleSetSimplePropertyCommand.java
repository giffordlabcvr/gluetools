package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(commandWords="set",
		docoptUsages="<propertyName> <propertyValue>",
		description = "Set a simple property on the module config")
public final class ModuleSetSimplePropertyCommand extends ModuleConfigureCommand {

	private String propertyName;
	private String propertyValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyName = PluginUtils.configureStringProperty(configElem, "propertyName", true);
		propertyValue = PluginUtils.configureStringProperty(configElem, "propertyValue", true);
	}

	protected final void updateDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		if(!module.getModulePlugin(cmdContext.getGluetoolsEngine(), false).getSimplePropertyNames().contains(propertyName)) {
			throw new ModuleException(ModuleException.Code.NO_SUCH_MODULE_PROPERTY, propertyName);
		}
		Element docElem = modulePluginDoc.getDocumentElement();
		List<Element> elements = GlueXmlUtils.findChildElements(docElem, propertyName);
		elements.forEach(e -> docElem.removeChild(e));
		GlueXmlUtils.appendElementWithText(docElem, propertyName, propertyValue);
	}
	
	@SuppressWarnings("rawtypes")
	@CompleterClass
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
