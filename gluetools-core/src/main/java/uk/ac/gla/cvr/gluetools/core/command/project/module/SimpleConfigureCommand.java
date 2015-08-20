package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsageGenerator;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsageGeneratorClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


public abstract class SimpleConfigureCommand<P extends ModulePlugin<P>> extends ConfigureCommand<P> {

	private String propertyName;
	private String propertyValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyName = PluginUtils.configureStringProperty(configElem, "propertyName", true);
		List<String> availablePropertyNames = availablePropertyNames(this.getClass());
		if(!availablePropertyNames.contains(propertyName)) {
			throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, "propertyName", 
					"Available property names: "+availablePropertyNames, propertyName);
		}
		propertyValue = PluginUtils.configureStringProperty(configElem, "propertyValue", true);
	}

	@SuppressWarnings("rawtypes")
	private static List<String> availablePropertyNames(Class<? extends Command> cmdClass) {
		return Arrays.asList(cmdClass.getAnnotation(SimpleConfigureCommandClass.class).propertyNames());
	}

	@SuppressWarnings("rawtypes")
	@CommandUsageGeneratorClass
	public static class ConfigureCommandUsageGenerator extends CommandUsageGenerator {
		@Override
		public CommandUsage generateUsage(Class<? extends Command> cmdClass) {
			SimpleConfigureCommandClass cfgCmdClassAnno = cmdClass.getAnnotation(SimpleConfigureCommandClass.class);
			return new CommandUsage(new String[]{"configure"}, 
					new String[]{"<propertyName> <propertyValue>"}, cfgCmdClassAnno.description(), 
					new String[]{}, "Available property names: "+String.join(", ",cfgCmdClassAnno.propertyNames()), true, 
					new String[]{});
		}
	}
	
	protected final void updateDocument(CommandContext cmdContext, Document modulePluginDoc) {
		Element docElem = modulePluginDoc.getDocumentElement();
		List<Element> elements = GlueXmlUtils.findChildElements(docElem, propertyName);
		elements.forEach(e -> docElem.removeChild(e));
		GlueXmlUtils.appendElementWithText(docElem, propertyName, propertyValue);
	}
	
	@SuppressWarnings("rawtypes")
	@CompleterClass
	public static class PropertyNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, 
				Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				return availablePropertyNames(cmdClass);
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}
}
