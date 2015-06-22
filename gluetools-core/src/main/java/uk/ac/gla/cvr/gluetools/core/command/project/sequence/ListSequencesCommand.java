package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

// TODO -- make source optional, and then list all in a project

@PluginClass(elemName="list-sequences")
@CommandClass(description="List sequences in a source", // or in the project", 
	docoptUsages={"<sourceName>"}) 
public class ListSequencesCommand extends ProjectModeCommand {

	private String sourceName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureString(configElem, "sourceName/text()", true);
	}
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Sequence.SOURCE_PROPERTY, sourceName);
		return CommandUtils.runListCommand(cmdContext, Sequence.class, new SelectQuery(Sequence.class, exp));
	}

}
