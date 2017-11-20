package uk.ac.gla.cvr.gluetools.core.command.project.module.property;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleShowPropertyCommand.ModuleShowSimplePropertyResult;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(commandWords={"show", "property"},
		docoptUsages="<propertyPath>",
		docCategory="General module commands",
		description = "Show value of a property on the module config")
public final class ModuleShowPropertyCommand extends ModulePropertyCommand<ModuleShowSimplePropertyResult> {
	
	protected final ModuleShowSimplePropertyResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		checkPropertyPath(cmdContext, module);
		String elemName = resolveElemName();
		Element parentElem = resolveParentElem(modulePluginDoc);
		String propertyValue = GlueXmlUtils.getXPathString(parentElem, elemName+"/text()");
		return new ModuleShowSimplePropertyResult(getPropertyPath(), propertyValue);
	}

	@CompleterClass
	public static final class Completer extends PropertyNameCompleter {}
	
	public static final class ModuleShowSimplePropertyResult extends MapResult {

		public ModuleShowSimplePropertyResult(String propertyPath, String propertyValue) {
			super("moduleShowPropertyResult", mapBuilder()
					.put("propertyPath", propertyPath)
					.put("propertyValue", propertyValue)
					.build());
		}
		
	}
 	
}
