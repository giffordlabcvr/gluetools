package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.Command;
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

	@SuppressWarnings("rawtypes")
	public static WebdocsModuleTypeDocumentation createDocumentation(String moduleTypeName) {
		ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
		PluginFactory<ModulePlugin<?>>.PluginClassInfo pluginClassInfo = pluginFactory.getPluginClassInfo(moduleTypeName);

		WebdocsModuleTypeDocumentation docPojo = new WebdocsModuleTypeDocumentation();
		docPojo.name = moduleTypeName;
		docPojo.description = pluginClassInfo.getDescription();
		ModulePlugin<?> modulePlugin = pluginClassInfo.getExampleInstance();

		List<Class<? extends Command>> providedCommandClasses = modulePlugin.getProvidedCommandClasses();
		
		Map<String, List<WebdocsCommandSummary>> catNameToSummaries = providedCommandClasses.stream().
			map(cls -> WebdocsCommandSummary.createSummary(cls)).
			collect(Collectors.groupingBy(wcs -> wcs.docCategory));
		
		catNameToSummaries.forEach( (name, summaries) -> {
			summaries.sort(new Comparator<WebdocsCommandSummary>() {

				@Override
				public int compare(WebdocsCommandSummary o1, WebdocsCommandSummary o2) {
					return o1.cmdWordID.compareTo(o2.cmdWordID);
				}});
			WebdocsCommandCategory category = WebdocsCommandCategory.create(name, summaries);
			if(category.description.equals("Type-specific module commands")) {
				docPojo.commandCategories.add(0, category);
			} else {
				docPojo.commandCategories.add(category);
			}
		});
		
		return docPojo;
	}
	
}
