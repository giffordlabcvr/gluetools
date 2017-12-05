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
