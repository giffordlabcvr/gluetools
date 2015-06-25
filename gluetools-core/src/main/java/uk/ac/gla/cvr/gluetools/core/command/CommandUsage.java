package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class CommandUsage {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");

	
	public static String docoptStringForCmdClass(Class<? extends Command> cmdClass, 
			boolean singleWordCmd) {
		CommandClass cmdClassAnno = cmdClass.getAnnotation(CommandClass.class);
		StringBuffer buf = new StringBuffer();
		buf.append("Usage: ");
		List<String> docoptUsages = new ArrayList<String>(Arrays.asList(cmdClassAnno.docoptUsages()));
		for(int i = 0; i < docoptUsages.size(); i++) {
			String usageLine = docoptUsages.get(i);
			if(singleWordCmd) {
				buf.append("command");
			} else {
				buf.append(String.join(" ", cmdClassAnno.commandWords()));
			}
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

	public static String furtherHelpForCmdClass(Class<? extends Command> cmdClass) {
		return cmdClass.getAnnotation(CommandClass.class).furtherHelp();
	}
	
	public static String[] cmdWordsForCmdClass(Class<? extends Command> cmdClass) {
		return cmdClass.getAnnotation(CommandClass.class).commandWords();
	}
	
	public static Element docElemForCmdClass(Class<? extends Command> cmdClass) {
		String[] cmdWords = cmdWordsForCmdClass(cmdClass);
		Element elem = XmlUtils.documentWithElement(cmdWords[0]);
		for(int i = 1; i < cmdWords.length; i++) {
			elem = XmlUtils.appendElement(elem, cmdWords[i]);
		}
		return elem;
	}

	public static CommandCompleter commandCompleterForCmdClass(Class<? extends Command> cmdClass) {
		Class<?> completerClass = Arrays.asList(cmdClass.getClasses()).stream().
				filter(c -> c.getAnnotation(CompleterClass.class) != null).findFirst().orElse(null);
		if(completerClass == null) {
			return null;
		}
		try {
			return (CommandCompleter) completerClass.getConstructor().newInstance();
		} catch(Exception e) {
			logger.warning("Failed to instantiate command completer class "+completerClass.getCanonicalName());
			logger.warning(e.getClass().getCanonicalName()+": "+e.getMessage());
		}
		return null;
	}
	
}
