package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(commandWords={"set", "property"},
		docoptUsages="<propertyName> <propertyValue>",
		description = "Set a simple property on the module config")
public final class ModuleSetSimplePropertyCommand extends ModuleSimplePropertyCommand<OkResult> 
	implements ModuleUpdateDocumentCommand {

	private String propertyValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyValue = PluginUtils.configureStringProperty(configElem, "propertyValue", true);
	}

	protected final OkResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		checkPropertyName(cmdContext, module);
		Element docElem = modulePluginDoc.getDocumentElement();
		List<Element> elements = GlueXmlUtils.findChildElements(docElem, getPropertyName());
		elements.forEach(e -> docElem.removeChild(e));
		GlueXmlUtils.appendElementWithText(docElem, getPropertyName(), propertyValue);
		return CommandResult.OK;
	}

	
	@CompleterClass
	public static final class Completer extends PropertyNameCompleter {}
}