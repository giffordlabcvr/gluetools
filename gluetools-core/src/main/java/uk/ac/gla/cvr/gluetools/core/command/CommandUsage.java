package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

public class CommandUsage {

	public static String docoptStringForCmdClass(Class<? extends Command> cmdClass) {
		String identifier = cmdClass.getAnnotation(PluginClass.class).elemName();
		CommandClass cmdClassAnno = cmdClass.getAnnotation(CommandClass.class);
		StringBuffer buf = new StringBuffer();
		buf.append("Usage: ");
		List<String> docoptUsages = new ArrayList<String>(Arrays.asList(cmdClassAnno.docoptUsages()));
		for(int i = 0; i < docoptUsages.size(); i++) {
			String usageLine = docoptUsages.get(i);
			buf.append(identifier);
			if(usageLine.trim().length() > 0) {
				buf.append(" ").append(usageLine);
			}
			buf.append("\n");
			if(i < docoptUsages.size() - 1) {
				buf.append("       ");
			}
		}
		buf.append("\n");
		List<String> docoptOptions = new ArrayList<String>(Arrays.asList(cmdClassAnno.docoptOptions()));
		if(!docoptOptions.isEmpty()) {
			buf.append("Options:\n");
			for(String option: docoptOptions) {
				buf.append("  ").append(option).append("\n");
			}
			buf.append("\n");
		}
		return buf.toString();
	}

	public static String descriptionForCmdClass(Class<? extends Command> cmdClass) {
		return cmdClass.getAnnotation(CommandClass.class).description();
	}

	public static String commandForCmdClass(Class<? extends Command> cmdClass) {
		return cmdClass.getAnnotation(PluginClass.class).elemName();
	}

}
