package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


@CommandClass( 
		commandWords={"show", "configuration"}, 
		docoptUsages={},
		description="Show the current configuration of this module") 
public final class ModuleShowConfigurationCommand extends ModuleDocumentCommand<ConsoleCommandResult> {

	@Override
	protected ConsoleCommandResult processDocument(CommandContext cmdContext,
			Module module, Document modulePluginDoc) {
		return new ConsoleCommandResult(new String(GlueXmlUtils.prettyPrint(modulePluginDoc)));
	}
}
