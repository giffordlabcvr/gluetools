package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public class WebdocsListModuleTypesResult extends BaseTableResult<String>{

	public WebdocsListModuleTypesResult(List<String> moduleTypeNames) {
		super("webdocsListModuleTypesResult", moduleTypeNames, 
				column("name", name -> name),
				column("description", name -> retrieveDescription(name)));
	}

	private static String retrieveDescription(String name) {
		ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
		return pluginFactory.getPluginClassInfo(name).getDescription();
	}

}
