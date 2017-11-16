package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;


@CommandClass( 
	commandWords={"webdocs", "list-module-types"}, 
	docoptUsages={""},
	metaTags={}, 
	description = "")
public class WebdocsListModuleTypesCommand extends WebdocsCommand<WebdocsListModuleTypesResult> {

	@Override
	public WebdocsListModuleTypesResult execute(CommandContext cmdContext) {
		ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
		List<String> moduleTypeNames = new ArrayList<String>(pluginFactory.getElementNames());
		moduleTypeNames.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		moduleTypeNames = moduleTypeNames.stream()
				.filter(s -> pluginFactory.getPluginClassInfo(s).includeInWebDocs())
				.collect(Collectors.toList());
		return new WebdocsListModuleTypesResult(moduleTypeNames);
	}
	
}
