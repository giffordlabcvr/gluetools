package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.help.GroupHelpLine;
import uk.ac.gla.cvr.gluetools.core.command.console.help.HelpLine;
import uk.ac.gla.cvr.gluetools.core.command.console.help.SpecificCommandHelpLine;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

// TODO Plugins should be able to add their own commands.
public abstract class CommandFactory {

private static Multiton factories = new Multiton();
	
	public static <F extends CommandFactory,
		C extends Multiton.Creator<F>> F get(C creator) {
		return factories.get(creator);
	}
	
	private CommandTreeNode rootNode = new CommandTreeNode();
	
	protected CommandFactory() {
	}
	
	protected void registerCommandClass(Class<? extends Command> cmdClass) {
		CommandClass cmdClassAnno = cmdClass.getAnnotation(CommandClass.class);
		if(cmdClassAnno == null) { throw new RuntimeException("No CommandClass annotation for "+cmdClass.getCanonicalName()); }
		if(EnterModeCommand.class.isAssignableFrom(cmdClass) && cmdClassAnno.docoptUsages().length != 1) {
			throw new RuntimeException("EnterModeCommand must have exactly one usage.");
		}
		rootNode.registerCommandClass(new LinkedList<String>(Arrays.asList(cmdClassAnno.commandWords())), cmdClass);
	}

	private class CommandTreeNode {
		Map<String, CommandTreeNode> childNodes = new LinkedHashMap<String, CommandTreeNode>();
		CommandPluginFactory cmdPluginFactory = new CommandPluginFactory();
		GroupHelpLine groupHelpLine;
		
		// fullHelp means expand group helps to cover sub groups.
		private List<HelpLine> helpLines(List<String> commandWords, boolean fullHelp) {
			List<HelpLine> helpLines = new ArrayList<HelpLine>();
			if(commandWords.size() == 1) {
				String finalWord = commandWords.get(0);
				Class<? extends Command> cmdClass = cmdPluginFactory.classForElementName(finalWord);
				if(cmdClass != null) {
					helpLines.add(new SpecificCommandHelpLine(cmdClass));
				}
			}
			if(commandWords.size() == 0) {
				if(groupHelpLine != null && !fullHelp) {
					helpLines.add(groupHelpLine);
				} else { 
					childNodes.values().stream().forEach(c -> helpLines.addAll(c.helpLines(commandWords, false)));
					cmdPluginFactory.getRegisteredClasses().forEach(c -> helpLines.add(new SpecificCommandHelpLine(c)));
				}
			} else {
				String firstWord = commandWords.remove(0);
				CommandTreeNode treeNode = childNodes.get(firstWord);
				if(treeNode != null) {
					boolean newFullHelp = commandWords.isEmpty();
					helpLines.addAll(treeNode.helpLines(commandWords, newFullHelp));
				}
			}
			return helpLines;
		}
		
		private void registerCommandClass(List<String> commandWords, Class<? extends Command> cmdClass) {
			if(commandWords.size() == 1) {
				String finalWord = commandWords.get(0);
				if(cmdPluginFactory.containsElementName(finalWord)) {
					throw new RuntimeException("Two command classes are using the same command words");
				}
				cmdPluginFactory.registerCommandClass(finalWord, cmdClass);
			} else {
				CommandTreeNode treeNode = childNodes.computeIfAbsent(commandWords.remove(0), word -> new CommandTreeNode());
				treeNode.registerCommandClass(commandWords, cmdClass);
			}
		}
		
		private Class<? extends Command> identifyCommandClass(List<String> commandWords) {
			if(commandWords.isEmpty()) { return null; }
			String firstWord = commandWords.remove(0);
			Class<? extends Command> cmdClass = cmdPluginFactory.classForElementName(firstWord);
			if(cmdClass != null) {
				return cmdClass;
			}
			CommandTreeNode treeNode = childNodes.get(firstWord);
			if(treeNode == null) {
				return null;
			}
			return treeNode.identifyCommandClass(commandWords);
		}
			
		private Command commandFromElement(List<CommandMode> commandModeStack,
			PluginConfigContext pluginConfigContext, Element element) {
			String nodeName = element.getNodeName();
			if(cmdPluginFactory.containsElementName(nodeName)) {
				Class<? extends Command> cmdClass = cmdPluginFactory.classForElementName(nodeName);
				for(int i = commandModeStack.size() - 1; i >= 0; i--) {
					commandModeStack.get(i).addModeConfigToCommandElem(cmdClass, element);
				}
				return cmdPluginFactory.createFromElement(pluginConfigContext, element);
			} else {
				CommandTreeNode treeNode = childNodes.get(nodeName);
				if(treeNode == null) {
					return null;
				}
				List<Element> childElements = XmlUtils.findChildElements(element);
				if(childElements.size() != 1) {
					return null;
				} else {
					return treeNode.commandFromElement(commandModeStack, pluginConfigContext, childElements.get(0));
				}
			}
		}

		public void addGroupHelp(LinkedList<String> commandWords,
				GroupHelpLine groupHelpLine) {
			if(commandWords.size() == 0) {
				if(this.groupHelpLine != null) {
					throw new RuntimeException("Group help line added twice.");
				}
				this.groupHelpLine = groupHelpLine;
			} else {
				CommandTreeNode treeNode = childNodes.computeIfAbsent(commandWords.remove(0), word -> new CommandTreeNode());
				treeNode.addGroupHelp(commandWords, groupHelpLine);
			}
			
		}

	}
	
	private class CommandPluginFactory extends PluginFactory<Command> {
		public void registerCommandClass(String finalWord, Class<? extends Command> cmdClass) {
			this.registerPluginClass(finalWord, cmdClass);
		}
	}

	public Command commandFromElement(List<CommandMode> commandModeStack,
			PluginConfigContext pluginConfigContext, Element element) {
		return rootNode.commandFromElement(commandModeStack, pluginConfigContext, element);
	}

	public Class<? extends Command> identifyCommandClass(List<String> commandWords) {
		return rootNode.identifyCommandClass(new LinkedList<String>(commandWords));
	}


	public List<String> getElementNames() {
		List<String> names = new ArrayList<String>();
		names.addAll(rootNode.childNodes.keySet());
		names.addAll(rootNode.cmdPluginFactory.getElementNames());
		return names;
	}

	public List<HelpLine> helpLinesForCommandWords(List<String> commandWords) {
		return rootNode.helpLines(new LinkedList<String>(commandWords), false);
	}

	protected void addGroupHelp(List<String> commandWords, String description) {
		GroupHelpLine groupHelpLine = new GroupHelpLine(commandWords, description);
		rootNode.addGroupHelp(new LinkedList<String>(commandWords), groupHelpLine);
	}
	
}
