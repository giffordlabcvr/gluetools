package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSetting;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


public class CommandContext {
	
	private GluetoolsEngine gluetoolsEngine;
	private List<ObjectContext> objectContextStack = new LinkedList<ObjectContext>();
	private String description;
	
	public CommandContext(GluetoolsEngine gluetoolsEngine, String description) {
		super();
		this.gluetoolsEngine = gluetoolsEngine;
		this.description = description;
	}

	private List<CommandMode<?>> commandModeStack = new ArrayList<CommandMode<?>>();
	private Optional<CommandContextListener> commandContextListener = Optional.empty();
	
	public void pushCommandMode(CommandMode<?> commandMode) {
		commandMode.setParentCommandMode(peekCommandMode());
		commandModeStack.add(0, commandMode);
		if(commandMode instanceof DbContextChangingMode) {
			ServerRuntime serverRuntime = ((DbContextChangingMode) commandMode).getNewServerRuntime();
			objectContextStack.add(0, GlueDataObject.createObjectContext(serverRuntime));
		}
		commandContextListener.ifPresent(c -> c.commandModeChanged());
	}

	public String getDescription() {
		return description;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ModeCloser pushCommandMode(String... words) {
		String enterModeCommandWord = words[0];
		Class<? extends Command> enterModeCmdClass = 
				peekCommandMode().getCommandFactory().identifyCommandClass(this, 
				Collections.singletonList(enterModeCommandWord));
		EnterModeCommandDescriptor enterModeCommandDescriptor = 
				EnterModeCommandDescriptor.getDescriptorForClass(enterModeCmdClass);
		String[] enterModeArgNames = enterModeCommandDescriptor.enterModeArgNames();
		if(enterModeCmdClass.getAnnotation(EnterModeCommandClass.class) == null) {
			throw new CommandException(Code.NOT_A_MODE_COMMAND, String.join(" ", words), getModePath());
		}
		CommandBuilder cmdBuilder = cmdBuilder(enterModeCmdClass);
		for(int i = 0; i < enterModeArgNames.length; i++) {
			cmdBuilder.set(enterModeArgNames[i], words[i+1]);
		}
		cmdBuilder.execute();
		return new ModeCloser();
	}
	
	public class ModeCloser implements AutoCloseable {
		@Override
		public void close() {
			popCommandMode();
		}
	}
	
	public CommandMode<?> popCommandMode() {
		CommandMode<?> commandMode = commandModeStack.remove(0);
		if(commandMode instanceof DbContextChangingMode) {
			objectContextStack.remove(0);
		}
		commandMode.exit();
		commandContextListener.ifPresent(c -> c.commandModeChanged());
		return commandMode;
	}
	
	public CommandMode<?> peekCommandMode() {
		if(commandModeStack.isEmpty()) {
			return null;
		}
		return commandModeStack.get(0);
	}
	
	public void setCommandContextListener(CommandContextListener listener) {
		commandContextListener = Optional.ofNullable(listener);
	}

	public String getModePath() {
		List<String> modeIds = 
				commandModeStack.stream().
				map(mode -> mode.getRelativeModePath()).collect(Collectors.toList());
		Collections.reverse(modeIds);
		String path = String.join("", modeIds);
		if(path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	
	
	public GluetoolsEngine getGluetoolsEngine() {
		return gluetoolsEngine;
	}
	
	
	@SuppressWarnings("rawtypes")
	public Class<? extends Command> commandClassFromElement(Element element) {
		Element current = element;
		List<String> elNames = new LinkedList<String>();
		while(current != null) {
			elNames.add(current.getNodeName());
			List<Element> childElems = GlueXmlUtils.findChildElements(current);
			if(!childElems.isEmpty()) {
				current = childElems.get(0);
			} else {
				current = null;
			}
		}
		return peekCommandMode().getCommandFactory().identifyCommandClass(this, elNames);
	}
	
	public Command<?> commandFromElement(Element element) {
		CommandFactory commandFactory = peekCommandMode().getCommandFactory();
		return commandFactory.commandFromElement(this, commandModeStack, gluetoolsEngine.createPluginConfigContext(), element);
	}

	public ObjectContext getObjectContext() {
		return objectContextStack.get(0);
	}

	public void commit() {
		getObjectContext().commitChanges();
	}
	
	public <R extends CommandResult, C extends Command<R>> CommandBuilder<R, C> cmdBuilder(Class<C> cmdClass) {
		return new CommandBuilder<R, C>(this, cmdClass);
	}

	@SuppressWarnings("rawtypes")
	public void checkCommmandIsExecutable(Class<? extends Command> cmdClass) {
	}

	public String getProjectSettingValue(ProjectSettingOption projectSettingOption) {
		ProjectSetting projectSetting = 
				GlueDataObject.lookup(getObjectContext(), ProjectSetting.class, 
						ProjectSetting.pkMap(projectSettingOption.name()), true);
		String valueText;
		if(projectSetting == null) {
			valueText = projectSettingOption.getDefaultValue();
		} else {
			valueText = projectSetting.getValue();
		}
		return valueText;
	}
	
}
