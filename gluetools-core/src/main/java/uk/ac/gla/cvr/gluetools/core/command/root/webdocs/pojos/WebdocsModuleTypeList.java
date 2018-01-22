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

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginGroup;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

@PojoDocumentClass
public class WebdocsModuleTypeList {

	@PojoDocumentListField(itemClass = WebdocsModuleTypeCategory.class)
	public List<WebdocsModuleTypeCategory> moduleTypeCategories = new ArrayList<WebdocsModuleTypeCategory>();

	@SuppressWarnings("rawtypes")
	public static WebdocsModuleTypeList create() {
		WebdocsModuleTypeList moduleTypeList = new WebdocsModuleTypeList();
		
		ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
		
		Map<ModulePluginGroup, TreeSet<String>> pluginGroupToElemNames = 
				
				pluginFactory.getModulePluginGroupRegistry().getPluginGroupToElemNames();
		
		pluginGroupToElemNames.forEach((pluginGroup, elemNames) -> {
			
			List<WebdocsModuleTypeSummary> moduleTypeSummaries = new ArrayList<WebdocsModuleTypeSummary>();
			
			for(String elemName: elemNames) {
				WebdocsModuleTypeSummary moduleTypeSummary = new WebdocsModuleTypeSummary();
				PluginFactory.PluginClassInfo pluginClassInfo = pluginFactory.getPluginClassInfo(elemName);
				if(!pluginClassInfo.includeInWebDocs()) {
					return;
				}
				moduleTypeSummary.name = elemName;
				moduleTypeSummary.description = pluginClassInfo.getDescription();
				moduleTypeSummaries.add(moduleTypeSummary);
			}
			
			moduleTypeList.moduleTypeCategories.add(
					WebdocsModuleTypeCategory.create(pluginGroup.getDescription(), moduleTypeSummaries));
		});
		
		return moduleTypeList;
	}
	
}
