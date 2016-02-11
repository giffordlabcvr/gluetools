package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleShowSimplePropertyCommand.ModuleShowSimplePropertyResult;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(commandWords={"show", "property"},
		docoptUsages="<propertyName>",
		description = "Show value of a simple property on the module config")
public final class ModuleShowSimplePropertyCommand extends ModuleSimplePropertyCommand<ModuleShowSimplePropertyResult> {
	
	protected final ModuleShowSimplePropertyResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		checkPropertyName(cmdContext, module);
		Element docElem = modulePluginDoc.getDocumentElement();
		String propertyName = getPropertyName();
		String propertyValue = GlueXmlUtils.getXPathString(docElem, propertyName+"/text()");
		return new ModuleShowSimplePropertyResult(propertyName, propertyValue);
	}

	@CompleterClass
	public static final class Completer extends PropertyNameCompleter {}
	
	public static final class ModuleShowSimplePropertyResult extends MapResult {

		public ModuleShowSimplePropertyResult(String propertyName, String propertyValue) {
			super("moduleShowSimplePropertyResult", mapBuilder()
					.put("propertyName", propertyName)
					.put("propertyValue", propertyValue)
					.build());
		}
		
	}
 	
}
