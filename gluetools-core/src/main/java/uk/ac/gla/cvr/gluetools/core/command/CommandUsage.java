package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class CommandUsage {

	public static String docoptStringForCmdClass(Class<? extends Command> cmdClass, 
			String specialUsageExtension, boolean singleWordCmd) {
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
				if(specialUsageExtension != null) {
					buf.append(" [").append("<").append(specialUsageExtension).append(">...]");
				}
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

}
