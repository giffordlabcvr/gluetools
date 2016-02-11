package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleSimplePropertyCommand.PropertyNameCompleter;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(commandWords={"unset","property"},
		docoptUsages="<propertyName>",
		description = "Unset a simple property on the module config")
public final class ModuleUnsetSimplePropertyCommand extends ModuleSimplePropertyCommand<OkResult> 
	implements ModuleUpdateDocumentCommand {
	
	protected final OkResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		checkPropertyName(cmdContext, module);
		Element docElem = modulePluginDoc.getDocumentElement();
		List<Element> elements = GlueXmlUtils.findChildElements(docElem, getPropertyName());
		elements.forEach(e -> docElem.removeChild(e));
		return CommandResult.OK;
	}
	@CompleterClass
	public static final class Completer extends PropertyNameCompleter {}

}
