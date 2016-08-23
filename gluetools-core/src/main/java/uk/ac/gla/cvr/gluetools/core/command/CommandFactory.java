package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.feature.FeatureModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceSequenceModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.ProjectSchemaModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table.TableSequencesModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.Multiton;
import uk.ac.gla.cvr.gluetools.utils.Multiton.Creator;

// TODO Plugins should be able to add their own commands.
// TODO consider wrapping a CommandType around the Class<? extends Command>.
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
	

	@SuppressWarnings("rawtypes")
	public void registerCommandClass(Class<? extends Command> cmdClass) {
		CommandUsage cmdUsage = CommandUsage.commandUsageForCmdClass(cmdClass);
		if(cmdUsage == null) { throw new RuntimeException("No CommandUsage defined for "+cmdClass.getCanonicalName()); }
		cmdUsage.validate(cmdClass);
		rootNode.registerCommandClass(new LinkedList<String>(Arrays.asList(cmdUsage.commandWords())), cmdClass);
	}

	private class CommandTreeNode {
		Map<String, CommandTreeNode> childNodes = new LinkedHashMap<String, CommandTreeNode>();
		CommandPluginFactory cmdPluginFactory = new CommandPluginFactory();
		GroupHelpLine groupHelpLine;
		boolean modeWrappable = false;
		
		
		@SuppressWarnings({ "rawtypes" })
		public void collectCommandClasses(List<Class<? extends Command>> cmdClasses) {
			cmdClasses.addAll(cmdPluginFactory.getRegisteredClasses());
			childNodes.values().forEach(cn -> cn.collectCommandClasses(cmdClasses));
		}
		
		// fullHelp means expand group helps to cover sub groups.
		private List<HelpLine> helpLines(List<String> commandWords, boolean fullHelp, boolean requireModeWrappable) {
			List<HelpLine> helpLines = new ArrayList<HelpLine>();
			if(commandWords.size() == 1) {
				String finalWord = commandWords.get(0);
				@SuppressWarnings("rawtypes")
				Class<? extends Command> cmdClass = cmdPluginFactory.classForElementName(finalWord);
				if(cmdClass != null) {
					if(!CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.nonModeWrappable) || !requireModeWrappable) {
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
						if(!CommandUsage.hasMetaTagForCmdClass(c, CmdMeta.nonModeWrappable) || !requireModeWrappable) {
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
		
		@SuppressWarnings("rawtypes")
		private void registerCommandClass(List<String> commandWords, Class<? extends Command> cmdClass) {
			if(!CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.nonModeWrappable)) {
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
		
		@SuppressWarnings("rawtypes")
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
			
		@SuppressWarnings("rawtypes")
		private Command commandFromElement(List<CommandMode<?>> commandModeStack,
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
				List<Element> childElements = GlueXmlUtils.findChildElements(element);
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

		@SuppressWarnings("rawtypes")
		public List<CompletionSuggestion> getCommandWordSuggestions(ConsoleCommandContext cmdContext, 
				LinkedList<String> commandWords, String prefix,
				boolean commandCompleters, boolean requireModeWrappable, boolean includeOptions) {
			if(commandWords.size() == 0) {
				List<CompletionSuggestion> suggestions = new LinkedList<CompletionSuggestion>();
				Set<String> childNodeElemNames = childNodes.keySet();
				for(String childNodeElemName: childNodeElemNames) {
					if(childNodes.get(childNodeElemName).modeWrappable || !requireModeWrappable) {
						suggestions.add(new CompletionSuggestion(childNodeElemName, true));
					}
				}
				Set<String> elementNames = cmdPluginFactory.getElementNames();
				for(String elementName: elementNames) {
					Class<? extends Command> cmdClass = cmdPluginFactory.classForElementName(elementName);
					if(!CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.nonModeWrappable) || !requireModeWrappable) {
						suggestions.add(new CompletionSuggestion(elementName, true));
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
							if(!CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.nonModeWrappable) || !requireModeWrappable) {
								return cmdCompleter.completionSuggestions(cmdContext, cmdClass, commandWords, prefix, includeOptions);
							}
						}
					}
					return new LinkedList<CompletionSuggestion>();
				}
				if(requireModeWrappable && !treeNode.modeWrappable) {
					return new LinkedList<CompletionSuggestion>();
				} else {
					return treeNode.getCommandWordSuggestions(cmdContext, commandWords, prefix, commandCompleters, requireModeWrappable, includeOptions);
				}
			}
		}

	}
	
	@SuppressWarnings("rawtypes")
	private class CommandPluginFactory extends PluginFactory<Command> {
		public void registerCommandClass(String finalWord, Class<? extends Command> cmdClass) {
			this.registerPluginClass(finalWord, cmdClass);
		}
	}

	@SuppressWarnings("rawtypes")
	public Command commandFromElement(CommandContext cmdContext, List<CommandMode<?>> commandModeStack,
			PluginConfigContext pluginConfigContext, Element element) {
		refreshCommandTree(cmdContext);
		return rootNode.commandFromElement(commandModeStack, pluginConfigContext, element);
	}

	@SuppressWarnings("rawtypes")
	public Class<? extends Command> identifyCommandClass(CommandContext cmdContext, List<String> commandWords) {
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

	public List<CompletionSuggestion> getCommandWordSuggestions(ConsoleCommandContext cmdContext, List<String> lookupBasis, 
			String prefix, boolean commandCompleters, boolean requireModeWrappable, boolean includeOptions) {
		refreshCommandTree(cmdContext);
		ArrayList<CompletionSuggestion> suggestions = new ArrayList<CompletionSuggestion>(rootNode.getCommandWordSuggestions(cmdContext, 
				new LinkedList<String>(lookupBasis), prefix, commandCompleters, requireModeWrappable, includeOptions));
		Collections.sort(suggestions);
		return suggestions;
	}
	
	protected void refreshCommandTree(CommandContext cmdContext) {}
	protected void resetCommandTree() {
		rootNode = new CommandTreeNode();
	}
	protected void populateCommandTree() {}
	
	
	@SuppressWarnings("rawtypes")
	private static Map<String, String> commandMap = new LinkedHashMap<String, String>();
	private static int totalLegacyCompleter = 0;
	private static int totalNoCompleter = 0;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		List<Creator<? extends CommandFactory>> creators = 	Arrays.asList(
				AlignmentModeCommandFactory.creator,
				FeatureLocModeCommandFactory.creator,
				FeatureModeCommandFactory.creator,
				MemberModeCommandFactory.creator,
				ProjectModeCommandFactory.creator,
				ProjectSchemaModeCommandFactory.creator,
				ReferenceSequenceModeCommandFactory.creator,
				RootCommandFactory.creator,
				SequenceModeCommandFactory.creator,
				TableSequencesModeCommandFactory.creator,
				VariationModeCommandFactory.creator
		);
		creators.forEach(creator -> {
			CommandFactory cmdFactory = CommandFactory.get(creator);
			cmdFactory.verifyCommands();
		});
		commandMap.forEach((c, s) -> {
			System.out.println(c);
			System.out.println(s);
			System.out.println("--------");
		});
		System.out.println("Commands with no completer: "+totalNoCompleter);
		System.out.println("Commands with legacy completer: "+totalLegacyCompleter);

	}
	
	@SuppressWarnings("rawtypes")
	public void verifyCommands() {
		List<Class<? extends Command>> cmdClasses = new ArrayList<Class<? extends Command>>();
		rootNode.collectCommandClasses(cmdClasses);
		
		for(Class<? extends Command> cmdClass : cmdClasses) {
			CommandUsage usage = CommandUsage.commandUsageForCmdClass(cmdClass);
			String megaUsage = String.join(" ", usage.docoptUsages());
			if(megaUsage.trim().length() > 0) {
				CommandCompleter completer = CommandCompleter.commandCompleterForCmdClass(cmdClass);
				String className = cmdClass.getCanonicalName();
				if(!commandMap.containsKey(className)) {
					if(completer == null) {
						commandMap.put(cmdClass.getCanonicalName(), "NO COMPLETER");
						totalNoCompleter++;
					} else if(!(completer instanceof AdvancedCmdCompleter)) {
						commandMap.put(cmdClass.getCanonicalName(), "LEGACY COMPLETER");
						totalLegacyCompleter++;
					}
				}
			}
		}
	}
	
	
	

}
