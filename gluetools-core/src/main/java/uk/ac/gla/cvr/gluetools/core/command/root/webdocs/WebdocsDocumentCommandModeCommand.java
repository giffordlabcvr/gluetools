package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsCommandModeDocumentation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"webdocs", "document-command-mode"}, 
	docoptUsages={"<absoluteModePathID>"},
	metaTags={CmdMeta.webApiOnly, CmdMeta.suppressDocs}, 
	description = "")
public class WebdocsDocumentCommandModeCommand extends WebdocsCommand<PojoCommandResult<WebdocsCommandModeDocumentation>> {

	
	public static final String ABSOLUTE_MODE_PATH_ID = "absoluteModePathID";
	
	private String absoluteModePathID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.absoluteModePathID = PluginUtils.configureStringProperty(configElem, ABSOLUTE_MODE_PATH_ID, true);
	}

	
	@Override
	public PojoCommandResult<WebdocsCommandModeDocumentation> execute(CommandContext cmdContext) {
		return new PojoCommandResult<WebdocsCommandModeDocumentation>(WebdocsCommandModeDocumentation.create(absoluteModePathID));
	}

}
