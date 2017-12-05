package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
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
				commandSummaries.add(WebdocsCommandSummary.createSummary((Class<? extends Command>) cmdClass));
			});
			docPojo.commandCategories.add(WebdocsCommandCategory.create(cmdGroup.getDescription(), commandSummaries));
		});
		
		return docPojo;
	}
	
}
