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
