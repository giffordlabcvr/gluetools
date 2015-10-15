package uk.ac.gla.cvr.gluetools.core.command.root;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandClass( 
		commandWords={"test", "command"},
		docoptUsages={"[-a] [-b] (-x <projectName> | -y)", 
				"-q <projectName>"},
		docoptOptions={"-a, --aOption  the A option",
				"-b, --bOption  the B option",
				"-x, --xOption  the X option",
				"-y, --yOption  the Y option",
				"-q, --qOption  the Q option",
				},
		description="test command")
public class TestCommand extends Command<OkResult>{

	@Override
	public OkResult execute(CommandContext cmdContext) {
		return new OkResult();
	}

	@CompleterClass
	public static class TestCompleter extends AdvancedCmdCompleter {
		@Override
		protected List<String> instantiateVariable(
				ConsoleCommandContext cmdContext, Map<String, Object> bindings,
				String variableName) {
			if(variableName.equals("projectName")) {
				return super.listNames(cmdContext, Project.class, Project.NAME_PROPERTY);
			}
			return null;
		}

		
	}

	
}
