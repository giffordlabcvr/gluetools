package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class CommandUsage {
	
	private String[] commandWords;
	private String[] docoptUsages;
	private String description;
	private String[] docoptOptions = {};
	private String furtherHelp = "";
	private boolean modeWrappable = true;
	
	public CommandUsage(String[] commandWords, String[] docoptUsages, String description) {
		super();
		this.commandWords = commandWords;
		this.description = description;
		this.docoptUsages = docoptUsages;
	}

	public CommandUsage(String[] commandWords, String[] docoptUsages,
			String description, String[] docoptOptions, String furtherHelp,
			boolean modeWrappable) {
		this(commandWords, docoptUsages, description);
		this.docoptOptions = docoptOptions;
		this.furtherHelp = furtherHelp;
		this.modeWrappable = modeWrappable;
	}
	
	private CommandUsage(CommandClass cmdClassAnno) {
		this(cmdClassAnno.commandWords(), 
				cmdClassAnno.docoptUsages(), 
				cmdClassAnno.description(), 
				cmdClassAnno.docoptOptions(), 
				cmdClassAnno.furtherHelp(), 
				cmdClassAnno.modeWrappable());
	}
	
	public String[] commandWords() {
		return commandWords;
	}

	public String[] docoptUsages() {
		return docoptUsages;
	}

	public String description() {
		return description;
	}

	public String[] docoptOptions() {
		return docoptOptions;
	}

	public String furtherHelp() {
		return furtherHelp;
	}

	public boolean modeWrappable() {
		return modeWrappable;
	}

	public static CommandUsage commandUsageForCmdClass(Class<? extends Command> cmdClass) {
		CommandClass cmdClassAnno = cmdClass.getAnnotation(CommandClass.class);
		if(cmdClassAnno != null) {
			return new CommandUsage(cmdClassAnno);
		}
		CommandUsageGenerator cmdUsgGenerator = CommandUsageGenerator.commandUsageGeneratorForCmdClass(cmdClass);
		if(cmdUsgGenerator != null) {
			return cmdUsgGenerator.generateUsage(cmdClass);
		}
		return null;
	}


	public static String docoptStringForCmdClass(Class<? extends Command> cmdClass, 
			boolean singleWordCmd) {
		CommandUsage cmdClassAnno = commandUsageForCmdClass(cmdClass);
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
		return commandUsageForCmdClass(cmdClass).description();
	}

	public static String furtherHelpForCmdClass(Class<? extends Command> cmdClass) {
		return commandUsageForCmdClass(cmdClass).furtherHelp();
	}
	
	public static String[] cmdWordsForCmdClass(Class<? extends Command> cmdClass) {
		return commandUsageForCmdClass(cmdClass).commandWords();
	}

	public static String[] docoptUsagesForCmdClass(Class<? extends Command> cmdClass) {
		return commandUsageForCmdClass(cmdClass).docoptUsages();
	}

	public static boolean modeWrappableForCmdClass(Class<? extends Command> cmdClass) {
		return commandUsageForCmdClass(cmdClass).modeWrappable();
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
