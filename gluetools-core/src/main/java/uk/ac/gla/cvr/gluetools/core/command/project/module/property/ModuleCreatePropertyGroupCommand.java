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
