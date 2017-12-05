package uk.ac.gla.cvr.gluetools.core.command.project.module.property;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"create", "property-group"},
		docoptUsages="<propertyPath>",
		description = "Create a property group in the module config")
public class ModuleCreatePropertyGroupCommand extends ModulePropertyCommand<CreateResult> implements ModuleUpdateDocumentCommand {

	@Override
	protected CreateResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		super.checkPropertyGroup(cmdContext, module);
		String elemName = resolveElemName();
		Element parentElem = resolveParentElem(modulePluginDoc);
		List<Element> elements = GlueXmlUtils.findChildElements(parentElem, elemName);
		if(elements.size() != 0) {
			return new CreateResult(PropertyGroup.class, 0);
		}
		GlueXmlUtils.appendElement(parentElem, elemName);
		return new CreateResult(PropertyGroup.class, 1);
	}

	@CompleterClass
	public static final class Completer extends PropertyGroupNameCompleter {}

	
}
