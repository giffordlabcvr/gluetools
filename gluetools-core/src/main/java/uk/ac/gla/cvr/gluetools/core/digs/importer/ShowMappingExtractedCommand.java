/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
		docoptUsages = { "" })
public class ShowMappingExtractedCommand extends ModuleDocumentCommand<ShowMappingExtractedResult> {

	@Override
	protected ShowMappingExtractedResult processDocument(
			CommandContext cmdContext, Module module, Document modulePluginDoc) {
		Element digsImporterElem = module.getConfigDoc().getDocumentElement();
		PluginConfigContext pluginConfigContext = cmdContext.getGluetoolsEngine().createPluginConfigContext();
		return new ShowMappingExtractedResult(cmdContext, new ArrayList<ImportExtractedFieldRule>(DigsImporter.initRulesMap(pluginConfigContext, digsImporterElem).values()));
	}

}
