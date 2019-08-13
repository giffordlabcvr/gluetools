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
package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class ModuleDocumentCommand<R extends CommandResult> extends ModuleModeCommand<R> {

	private boolean requireValidCurrentDocument = true;
	
	protected void setRequireValidCurrentDocument(boolean requireValidCurrentDocument) {
		this.requireValidCurrentDocument = requireValidCurrentDocument;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final R execute(CommandContext cmdContext) {
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(getModuleName()));
		Document currentDocument = null;
		try {
			currentDocument = module.getConfigDoc();
		} catch(GlueException ge) {
			if(this.requireValidCurrentDocument) {
				throw new CommandException(ge, Code.COMMAND_FAILED_ERROR, "Module document command cannot be executed as current document is invalid: "+ge.getLocalizedMessage());
			}
		}
		R result = processDocument(cmdContext, module, currentDocument);
		if(this instanceof ModuleUpdateDocumentCommand) {
			GlueXmlUtils.stripWhitespace(currentDocument);
			module.setConfig(GlueXmlUtils.prettyPrint(currentDocument));
			cmdContext.commit();
		}
		return result;
	}
	
	protected abstract R processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc);
	
	
}
