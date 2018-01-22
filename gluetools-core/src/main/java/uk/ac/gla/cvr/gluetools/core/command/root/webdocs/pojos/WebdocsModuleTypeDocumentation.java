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
package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

@PojoDocumentClass
public class WebdocsModuleTypeDocumentation {

	@PojoDocumentField
	public String name;
	
	@PojoDocumentField
	public String description;
	
	@PojoDocumentListField(itemClass = WebdocsCommandCategory.class)
	public List<WebdocsCommandCategory> commandCategories = new ArrayList<WebdocsCommandCategory>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static WebdocsModuleTypeDocumentation createDocumentation(String moduleTypeName) {
		ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
		PluginFactory<ModulePlugin<?>>.PluginClassInfo pluginClassInfo = pluginFactory.getPluginClassInfo(moduleTypeName);

		WebdocsModuleTypeDocumentation docPojo = new WebdocsModuleTypeDocumentation();
		docPojo.name = moduleTypeName;
		docPojo.description = pluginClassInfo.getDescription();
		ModulePlugin<?> modulePlugin = pluginClassInfo.getExampleInstance();

		Map<CommandGroup, TreeSet<Class<?>>> cmdGroupToCmdClasses = modulePlugin.getCommandGroupRegistry().getCmdGroupToCmdClasses();
		
		cmdGroupToCmdClasses.forEach((cmdGroup, setOfClasses) -> {
			List<WebdocsCommandSummary> commandSummaries = new ArrayList<WebdocsCommandSummary>();
			setOfClasses.forEach(cmdClass -> {
				if(CommandUsage.hasMetaTagForCmdClass((Class<? extends Command>) cmdClass, CmdMeta.suppressDocs)) {
					return;
				}
				commandSummaries.add(WebdocsCommandSummary.createSummary((Class<? extends Command>) cmdClass));
			});
			if(!commandSummaries.isEmpty()) {
				docPojo.commandCategories.add(WebdocsCommandCategory.create(cmdGroup.getId(), cmdGroup.getDescription(), commandSummaries));
			}
		});
		
		return docPojo;
	}
	
}
