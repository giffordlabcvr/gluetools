package uk.ac.gla.cvr.gluetools.core.digs.importer;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"import", "extracted"}, 
		description = "Import some rows from a DIGS \"Extracted\" table as GLUE sequences", 
		docoptUsages = { "<digsDbName>" },
		docoptOptions = { 
				},
		metaTags = {}	
)
public class ImportExtractedCommand extends DigsImporterCommand<ImportExtractedResult> implements ProvidedProjectModeCommand {

	public static final String DIGS_DB_NAME = "digsDbName";
	
	private String digsDbName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.digsDbName = PluginUtils.configureStringProperty(configElem, DIGS_DB_NAME, true);
	}

	@Override
	protected ImportExtractedResult execute(CommandContext cmdContext, DigsImporter digsImporter) {
		return digsImporter.importHits(cmdContext, digsDbName);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("digsDbName", new DigsDbNameInstantiator());
		}
		
	}
	
}
