package uk.ac.gla.cvr.gluetools.core.command.project.module.property;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"delete", "property-group"},
		docoptUsages="<propertyPath>",
		docCategory="General module commands",
		description = "Delete a property group from the module config")
public class ModuleDeletePropertyGroupCommand extends ModulePropertyCommand<DeleteResult> implements ModuleUpdateDocumentCommand {

	@Override
	protected DeleteResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		super.checkPropertyGroup(cmdContext, module);
		String elemName = resolveElemName();
		Element parentElem = resolveParentElem(modulePluginDoc);
		List<Element> elements = GlueXmlUtils.findChildElements(parentElem, elemName);
		if(elements.size() == 0) {
			return new DeleteResult(PropertyGroup.class, 0);
		}
		elements.forEach(e -> parentElem.removeChild(e));
		return new DeleteResult(PropertyGroup.class, 1);
	}

	@CompleterClass
	public static final class Completer extends PropertyGroupNameCompleter {}

}
