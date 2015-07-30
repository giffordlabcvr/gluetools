package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.help.GroupHelpLine;
import uk.ac.gla.cvr.gluetools.core.command.console.help.HelpLine;
import uk.ac.gla.cvr.gluetools.core.command.console.help.SpecificCommandHelpLine;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;
import uk.ac.gla.cvr.gluetools.utils.Multiton.Creator;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

// TODO Plugins should be able to add their own commands.
// TODO consider wrapping a CommandType around the Class<C extends Command>.
// cmdType should be used wherever cmdClass is now. This should allow more dynamic specification of command types.
public abstract class CommandFactory {

private static Multiton factories = new Multiton();
	
	public static <F extends CommandFactory,
		C extends Multiton.Creator<F>> F get(C creator) {
		return factories.get(creator);
	}

	public static <F extends CommandFactory> F get(Class<F> commandFactoryClass) {
		try {
			@SuppressWarnings("unchecked")
			Creator<F> creator = (Creator<F>) commandFactoryClass.getField("creator").get(null);
			return get(creator);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private CommandTreeNode rootNode;
	
	protected CommandFactory() {
		resetCommandTree();
		populateCommandTree();
	}
	

	protected void registerCommandClass(Class<? extends Command> cmdClass) {
		CommandUsage cmdUsage = CommandUsage.commandUsageForCmdClass(cmdClass);
		if(cmdUsage == null) { throw new RuntimeException("No CommandUsage defined for "+cmdClass.getCanonicalName()); }
		if(EnterModeCommand.class.isAssignableFrom(cmdClass) && cmdUsage.docoptUsages().length != 1) {
			throw new RuntimeException("EnterModeCommand must have exactly one docopt usage.");
		}
		rootNode.registerCommandClass(new LinkedList<String>(Arrays.asList(cmdUsage.commandWords())), cmdClass);
	}

	private class CommandTreeNode {
		Map<String, CommandTreeNode> childNodes = new LinkedHashMap<String, CommandTreeNode>();
		CommandPluginFactory cmdPluginFactory = new CommandPluginFactory();
		GroupHelpLine groupHelpLine;
		boolean modeWrappable = false;
		
		// fullHelp means expand group helps to cover sub groups.
		private List<HelpLine> helpLines(List<String> commandWords, boolean fullHelp, boolean requireModeWrappable) {
			List<HelpLine> helpLines = new ArrayList<HelpLine>();
			if(commandWords.size() == 1) {
				String finalWord = commandWords.get(0);
				Class<? extends Command> cmdClass = cmdPluginFactory.classForElementName(finalWord);
				if(cmdClass != null) {
					if(CommandUsage.modeWrappableForCmdClass(cmdClass) || !requireModeWrappable) {
						helpLines.add(new SpecificCommandHelpLine(cmdClass));
					}
				}
			}
			if(commandWords.size() == 0) {
				if(groupHelpLine != null && !fullHelp) {
					if(modeWrappable || !requireModeWrappable) {
						helpLines.add(groupHelpLine);
					}
				} else { 
					childNodes.values().stream().forEach(c -> helpLines.addAll(c.helpLines(commandWords, false, requireModeWrappable)));
					cmdPluginFactory.getRegisteredClasses().forEach(c -> { 
						if(CommandUsage.modeWrappableForCmdClass(c) || !requireModeWrappable) {
							helpLines.add(new SpecificCommandHelpLine(c));
						}
					});
				}
			} else {
				String firstWord = commandWords.remove(0);
				CommandTreeNode treeNode = childNodes.get(firstWord);
				if(treeNode != null) {
					boolean newFullHelp = commandWords.isEmpty();
					helpLines.addAll(treeNode.helpLines(commandWords, newFullHelp, requireModeWrappable));
				}
			}
			return helpLines;
		}
		
		private void registerCommandClass(List<String> commandWords, Class<? extends Command> cmdClass) {
			if(CommandUsage.modeWrappableForCmdClass(cmdClass)) {
				modeWrappable = true;
			}
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
				Command command = cmdPluginFactory.createFromElement(pluginConfigContext, element);
				command.setCmdElem(element);
				return command;
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

		public List<String> getCommandWordSuggestions(ConsoleCommandContext cmdContext, 
				LinkedList<String> commandWords, 
				boolean commandCompleters,
				boolean requireModeWrappable) {
			if(commandWords.size() == 0) {
				List<String> suggestions = new LinkedList<String>();
				Set<String> childNodeElemNames = childNodes.keySet();
				for(String childNodeElemName: childNodeElemNames) {
					if(childNodes.get(childNodeElemName).modeWrappable || !requireModeWrappable) {
						suggestions.add(childNodeElemName);
					}
				}
				Set<String> elementNames = cmdPluginFactory.getElementNames();
				for(String elementName: elementNames) {
					Class<? extends Command> cmdClass = cmdPluginFactory.classForElementName(elementName);
					if(CommandUsage.modeWrappableForCmdClass(cmdClass) || !requireModeWrappable) {
						suggestions.add(elementName);
					}
				}
				return suggestions;
			} else {
				String firstWord = commandWords.remove(0);
				CommandTreeNode treeNode = childNodes.get(firstWord);
				if(treeNode == null) {
					Class<? extends Command> cmdClass = cmdPluginFactory.classForElementName(firstWord);
					if(cmdClass != null && commandCompleters) {
						CommandCompleter cmdCompleter = CommandCompleter.commandCompleterForCmdClass(cmdClass);
						if(cmdCompleter != null) {
							if(CommandUsage.modeWrappableForCmdClass(cmdClass) || !requireModeWrappable) {
								return cmdCompleter.completionSuggestions(cmdContext, cmdClass, commandWords);
							}
						}
					}
					return new LinkedList<String>();
				}
				if(requireModeWrappable && !treeNode.modeWrappable) {
					return new LinkedList<String>();
				} else {
					return treeNode.getCommandWordSuggestions(cmdContext, commandWords, commandCompleters, requireModeWrappable);
				}
			}
		}

	}
	
	private class CommandPluginFactory extends PluginFactory<Command> {
		public void registerCommandClass(String finalWord, Class<? extends Command> cmdClass) {
			this.registerPluginClass(finalWord, cmdClass);
		}
	}

	public Command commandFromElement(CommandContext cmdContext, List<CommandMode> commandModeStack,
			PluginConfigContext pluginConfigContext, Element element) {
		refreshCommandTree(cmdContext);
		return rootNode.commandFromElement(commandModeStack, pluginConfigContext, element);
	}

	public Class<? extends Command> identifyCommandClass(ConsoleCommandContext cmdContext, List<String> commandWords) {
		refreshCommandTree(cmdContext);
		return rootNode.identifyCommandClass(new LinkedList<String>(commandWords));
	}
	

	public List<HelpLine> helpLinesForCommandWords(ConsoleCommandContext cmdContext, List<String> commandWords) {
		refreshCommandTree(cmdContext);
		return rootNode.helpLines(new LinkedList<String>(commandWords), false, cmdContext.isRequireModeWrappable());
	}

	protected void addGroupHelp(List<String> commandWords, String description) {
		GroupHelpLine groupHelpLine = new GroupHelpLine(commandWords, description);
		rootNode.addGroupHelp(new LinkedList<String>(commandWords), groupHelpLine);
	}

	public List<String> getCommandWordSuggestions(ConsoleCommandContext cmdContext, List<String> lookupBasis, 
			boolean commandCompleters, boolean requireModeWrappable) {
		refreshCommandTree(cmdContext);
		return rootNode.getCommandWordSuggestions(cmdContext, new LinkedList<String>(lookupBasis), commandCompleters, 
				requireModeWrappable);
	}
	
	protected void refreshCommandTree(CommandContext cmdContext) {}
	protected void resetCommandTree() {
		rootNode = new CommandTreeNode();
	}
	protected void populateCommandTree() {}

}
