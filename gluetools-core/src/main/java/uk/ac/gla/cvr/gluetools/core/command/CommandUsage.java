/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.Node;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptLexer;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptLexer.Token;

@SuppressWarnings("rawtypes")
public class CommandUsage {
	
	private String[] commandWords;
	private String[] docoptUsages;
	private String description;
	private String[] docoptOptions = {};
	private String furtherHelp = "";
	private String[] metaTags = {};
	
	public CommandUsage(String[] commandWords, String[] docoptUsages, String description) {
		super();
		this.commandWords = commandWords;
		this.description = description;
		this.docoptUsages = docoptUsages;
	}

	public CommandUsage(String[] commandWords, String[] docoptUsages,
			String description, String[] docoptOptions, String furtherHelp,
			String[] metaTags) {
		this(commandWords, docoptUsages, description);
		this.docoptOptions = docoptOptions;
		this.furtherHelp = furtherHelp;
		this.metaTags = metaTags;
	}
	
	private CommandUsage(CommandClass cmdClassAnno) {
		this(cmdClassAnno.commandWords(), 
				cmdClassAnno.docoptUsages(), 
				cmdClassAnno.description(), 
				cmdClassAnno.docoptOptions(), 
				cmdClassAnno.furtherHelp(),
				cmdClassAnno.metaTags());
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

	public boolean hasMetaTag(String metaTag) {
		return Arrays.asList(metaTags).contains(metaTag);
	}

	@SuppressWarnings("rawtypes")
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

	public static boolean hasMetaTagForCmdClass(Class<? extends Command> cmdClass, String metaTag) {
		return commandUsageForCmdClass(cmdClass).hasMetaTag(metaTag);
	}

	public void validate(Class<? extends Command> cmdClass) {
		try {
			String[] commandWords = commandWords();
			for(String commandWord: commandWords) {
				if(!commandWord.matches("[a-z][a-z0-9\\-]*")) {
					throw new RuntimeException("Invalid command word \""+commandWord+"\"");
				}
			}
			Map<Character, String> optionsMap = optionsMap();
			//System.out.println("command class: "+cmdClass.getCanonicalName());
			//System.out.println("command words: "+String.join(" ", commandWords));
			//System.out.println("options map: "+optionsMap);
			createFSM(optionsMap);
		} catch(Exception e) {
			throw new RuntimeException("Failed to validate command usage for "+cmdClass.getSimpleName(), e);
		}
	}

	public Node createFSM(Map<Character, String> optionsMap) {
		List<Node> startNodes = new ArrayList<Node>();
		for(String docoptUsage: docoptUsages) {
			List<Token> tokens = DocoptLexer.meaningfulTokens(DocoptLexer.lex(docoptUsage));
			//System.out.println("docoptUsage: "+docoptUsage);
			//System.out.println("tokens: "+tokens);
			//System.out.println("---------");
			startNodes.add(DocoptFSM.buildFSM(tokens, optionsMap));
		}
		Node mainNode = new Node();
		startNodes.forEach(startNode -> mainNode.nullTo(startNode));
		return mainNode;
	}
	
	public Map<Character, String> optionsMap() {
		Map<Character, String> optionsMap = new LinkedHashMap<Character, String>();
		for(String optionLine: docoptOptions) {
			String mainPart = optionLine.substring(0, optionLine.indexOf("  ")).trim();
			mainPart = mainPart.replaceAll("<[A-Za-z0-9]+>", "");
			String[] bits = mainPart.split(",");
			optionsMap.put(bits[0].trim().charAt(1), 
					bits[1].trim().replace("--", ""));
		}
		return optionsMap;
	}

	public String cmdWordID() {
		return String.join("_", commandWords());
	}

	
}
