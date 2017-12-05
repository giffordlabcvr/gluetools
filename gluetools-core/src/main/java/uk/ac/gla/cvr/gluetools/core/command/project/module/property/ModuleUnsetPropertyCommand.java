package uk.ac.gla.cvr.gluetools.core.command.project.module.property;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(commandWords={"unset","property"},
		docoptUsages="<propertyPath>",
		description = "Unset a property on the module config")
public final class ModuleUnsetPropertyCommand extends ModulePropertyCommand<OkResult> 
	implements ModuleUpdateDocumentCommand {
	
	protected final OkResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		checkPropertyPath(cmdContext, module);
		String elemName = resolveElemName();
		Element parentElem = resolveParentElem(modulePluginDoc); 
		List<Element> elements = GlueXmlUtils.findChildElements(parentElem, elemName);
		elements.forEach(e -> parentElem.removeChild(e));
		return CommandResult.OK;
	}
	@CompleterClass
	public static final class Completer extends PropertyNameCompleter {}

}
