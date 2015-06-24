package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.HelpCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.QuitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.SetDirectoryCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ShowDirectoryCommand;
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
		super();
		registerCommandClass(QuitCommand.class);
		registerCommandClass(ExitCommand.class);
		registerCommandClass(HelpCommand.class);
		registerCommandClass(SetDirectoryCommand.class);
		registerCommandClass(ShowDirectoryCommand.class);
	}
	
	protected void registerCommandClass(Class<? extends Command> cmdClass) {
		CommandClass cmdClassAnno = cmdClass.getAnnotation(CommandClass.class);
		if(cmdClassAnno == null) { throw new RuntimeException("No CommandClass annotation for "+cmdClass.getCanonicalName()); }
		String[] commandWords = cmdClassAnno.commandWords();
		CommandTreeNode treeNode = rootNode;
		int i = 0;
		for(i = 0; i < commandWords.length - 1; i++) {
			treeNode = treeNode.childNodes.computeIfAbsent(commandWords[i], word -> new CommandTreeNode());
		}
		String finalWord = commandWords[i];
		if(treeNode.cmdPluginFactory.containsElementName(finalWord)) {
			throw new RuntimeException("Two command classes are using the same command words");
		}
		treeNode.cmdPluginFactory.registerCommandClass(finalWord, cmdClass);
	}

	private class CommandTreeNode {
		Map<String, CommandTreeNode> childNodes = new LinkedHashMap<String, CommandTreeNode>();
		CommandPluginFactory cmdPluginFactory = new CommandPluginFactory();
	}
	
	private class CommandPluginFactory extends PluginFactory<Command> {
		public void registerCommandClass(String finalWord, Class<? extends Command> cmdClass) {
			this.registerPluginClass(finalWord, cmdClass);
		}
	}

	public Command commandFromElement(List<CommandMode> commandModeStack,
			PluginConfigContext pluginConfigContext, Element element) {
		Element currentElem = element;
		CommandTreeNode treeNode = rootNode;
		while(treeNode != null && !treeNode.cmdPluginFactory.containsElementName(currentElem.getNodeName())) {
			treeNode = treeNode.childNodes.get(currentElem.getNodeName());
			List<Element> childElements = XmlUtils.findChildElements(currentElem);
			if(childElements.size() != 1) {
				treeNode = null;
			} else {
				currentElem = childElements.get(0);
			}
		}
		if(treeNode == null) {
			return null;
		}
		Class<? extends Command> cmdClass = treeNode.cmdPluginFactory.classForElementName(currentElem.getNodeName());
		for(int i = commandModeStack.size() - 1; i >= 0; i--) {
			commandModeStack.get(i).addModeConfigToCommandElem(cmdClass, currentElem);
		}
		return treeNode.cmdPluginFactory.createFromElement(pluginConfigContext, currentElem);
	}

	public Class<? extends Command> identifyCommandClass(List<String> tokenStrings) {
		CommandTreeNode treeNode = rootNode;
		int tokenIndex = 0;
		while(treeNode != null && tokenIndex < tokenStrings.size() && 
				!treeNode.cmdPluginFactory.containsElementName(tokenStrings.get(tokenIndex))) {
			treeNode = treeNode.childNodes.get(tokenStrings.get(tokenIndex));
			tokenIndex++;
		}
		if(treeNode == null) {
			return null;
		}
		if(tokenIndex >= tokenStrings.size()) {
			return null;
		}
		return treeNode.cmdPluginFactory.classForElementName(tokenStrings.get(tokenIndex));
	}

	public List<String> getElementNames() {
		List<String> names = new ArrayList<String>();
		names.addAll(rootNode.childNodes.keySet());
		names.addAll(rootNode.cmdPluginFactory.getElementNames());
		return names;
	}
	
}
