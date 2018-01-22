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
package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsModuleTypeDocumentation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"webdocs", "document-module-type"}, 
	docoptUsages={"<moduleTypeName>"},
	metaTags={CmdMeta.webApiOnly, CmdMeta.suppressDocs}, 
	description = "")
public class WebdocsDocumentModuleTypeCommand extends WebdocsCommand<PojoCommandResult<WebdocsModuleTypeDocumentation>> {

	public static final String MODULE_TYPE_NAME = "moduleTypeName";
	
	private String moduleTypeName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.moduleTypeName = PluginUtils.configureStringProperty(configElem, MODULE_TYPE_NAME, true);
	}

	@Override
	public PojoCommandResult<WebdocsModuleTypeDocumentation> execute(CommandContext cmdContext) {
		return new PojoCommandResult<WebdocsModuleTypeDocumentation>(
				WebdocsModuleTypeDocumentation.createDocumentation(moduleTypeName));
	}

}
