package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
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
	
	@PojoDocumentListField(itemClass = WebdocsCommandDocumentation.class)
	public List<WebdocsCommandSummary> commands = new ArrayList<WebdocsCommandSummary>();

	@SuppressWarnings("rawtypes")
	public static WebdocsModuleTypeDocumentation createDocumentation(String moduleTypeName) {
		ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
		PluginFactory<ModulePlugin<?>>.PluginClassInfo pluginClassInfo = pluginFactory.getPluginClassInfo(moduleTypeName);

		WebdocsModuleTypeDocumentation docPojo = new WebdocsModuleTypeDocumentation();
		docPojo.name = moduleTypeName;
		docPojo.description = pluginClassInfo.getDescription();
		ModulePlugin<?> modulePlugin = pluginClassInfo.getExampleInstance();

		List<Class<? extends Command>> providedCommandClasses = modulePlugin.getProvidedCommandClasses();
		
		for(Class<? extends Command> cmdClass: providedCommandClasses) {
			docPojo.commands.add(WebdocsCommandSummary.createSummary(cmdClass));
		}
		
		return docPojo;
	}
	
}
