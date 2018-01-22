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
