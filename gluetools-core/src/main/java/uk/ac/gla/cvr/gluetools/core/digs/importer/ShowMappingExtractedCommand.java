package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass(
		commandWords={"show", "mapping", "extracted"}, 
		description = "Show mapping between DIGS \"Extracted\" and GLUE sequence fields", 
		docoptUsages = { "" }
)
public class ShowMappingExtractedCommand extends ModuleDocumentCommand<ShowMappingExtractedResult> {

	@Override
	protected ShowMappingExtractedResult processDocument(
			CommandContext cmdContext, Module module, Document modulePluginDoc) {
		Element digsImporterElem = module.getConfigDoc().getDocumentElement();
		PluginConfigContext pluginConfigContext = cmdContext.getGluetoolsEngine().createPluginConfigContext();
		return new ShowMappingExtractedResult(new ArrayList<ImportExtractedFieldRule>(DigsImporter.initRulesMap(pluginConfigContext, digsImporterElem).values()));
	}

}
