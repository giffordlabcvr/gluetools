package uk.ac.gla.cvr.gluetools.core.command.project.module.property;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(commandWords={"set", "property"},
		docoptUsages="<propertyPath> <propertyValue>",
		description = "Set a property on the module config")
public final class ModuleSetPropertyCommand extends ModulePropertyCommand<OkResult> 
	implements ModuleUpdateDocumentCommand {

	private String propertyValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyValue = PluginUtils.configureStringProperty(configElem, "propertyValue", true);
	}

	protected final OkResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		checkPropertyPath(cmdContext, module);
		String elemName = resolveElemName();
		Element parentElem = resolveParentElem(modulePluginDoc); 
		List<Element> elements = GlueXmlUtils.findChildElements(parentElem, elemName);
		for(Element e : elements) {
			parentElem.removeChild(e);
		}
		GlueXmlUtils.appendElementWithText(parentElem, elemName, propertyValue);
		return CommandResult.OK;
	}



	
	
	@CompleterClass
	public static final class Completer extends PropertyNameCompleter {}
}
