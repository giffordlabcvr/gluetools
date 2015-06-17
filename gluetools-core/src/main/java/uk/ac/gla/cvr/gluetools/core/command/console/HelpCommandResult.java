package uk.ac.gla.cvr.gluetools.core.command.console;

import java.util.Comparator;
import java.util.Formatter;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

public class HelpCommandResult extends ConsoleCommandResult {

	private List<Class<? extends Command>> cmdClasses;
	
	public HelpCommandResult(List<Class<? extends Command>> cmdClasses) {
		super();
		this.cmdClasses = cmdClasses;
	}

	@Override
	public String getResultAsConsoleText() {
		StringBuffer buf = new StringBuffer();
		// TODO make this adapt to terminal width
		try(Formatter formatter = new Formatter(buf)) {
			cmdClasses.stream().map(c -> new HelpLine(
					c.getAnnotation(PluginClass.class ).elemName(),
					c.getAnnotation(CommandClass.class).description()
					)).sorted(new Comparator<HelpLine>() {
						@Override
						public int compare(HelpLine o1, HelpLine o2) {
							return o1.command.compareTo(o2.command);
						}
					}).forEach(h -> {
						formatter.format("  %-20s - %-55s\n", h.command, h.description);
					});
		}
		buf.append("\nTo describe a specific command, use: help <command>\n");
		return buf.toString();
	}
	
	private class HelpLine {
		String command;
		String description;
		public HelpLine(String command, String description) {
			super();
			this.command = command;
			this.description = description;
		}
		
	}

}
