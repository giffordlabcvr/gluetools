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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;
import uk.ac.gla.cvr.gluetools.core.command.scripting.NashornContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSetting;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


public class CommandContext {
	
	private GluetoolsEngine gluetoolsEngine;
	private List<ObjectContext> objectContextStack = new LinkedList<ObjectContext>();
	private String description;
	private Map<CacheKey, GlueDataObject> uncommittedCache = new LinkedHashMap<CommandContext.CacheKey, GlueDataObject>();
	private List<CommandMode<?>> commandModeStack = new ArrayList<CommandMode<?>>();
	private XPath xpathEngine;
	private NashornContext nashornContext;
	
	public CommandContext(GluetoolsEngine gluetoolsEngine, String description) {
		super();
		this.gluetoolsEngine = gluetoolsEngine;
		this.description = description;
		this.xpathEngine = GlueXmlUtils.createXPathEngine();
	}

	
	public void pushCommandMode(CommandMode<?> commandMode) {
		commandMode.setParentCommandMode(peekCommandMode());
		commandModeStack.add(0, commandMode);
		if(commandMode instanceof DbContextChangingMode) {
			ServerRuntime serverRuntime = ((DbContextChangingMode) commandMode).getNewServerRuntime();
			objectContextStack.add(0, serverRuntime.getContext());
		}
	}

	public String getDescription() {
		return description;
	}

	public ModeCloser pushCommandMode(String... words) {
		pushCommandModeReturnNumWordsUsed(words);
		return new ModeCloser();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int pushCommandModeReturnNumWordsUsed(String... words) {
		if(words.length == 0) {
			return 0;
		}
		String enterModeCommandWord = words[0];
		int used = 1;
		Class<? extends Command> enterModeCmdClass = 
				peekCommandMode().getCommandFactory().identifyCommandClass(this, 
				Collections.singletonList(enterModeCommandWord));
		if(enterModeCmdClass == null) {
			throw new CommandException(Code.UNKNOWN_MODE_PATH, String.join("/", words));
		}
		if(enterModeCmdClass.getAnnotation(EnterModeCommandClass.class) == null) {
			throw new CommandException(Code.NOT_A_MODE_COMMAND, String.join(" ", words), getModePath());
		}
		EnterModeCommandDescriptor enterModeCommandDescriptor = 
				EnterModeCommandDescriptor.getDescriptorForClass(enterModeCmdClass);
		String[] enterModeArgNames = enterModeCommandDescriptor.enterModeArgNames();
		if(words.length < enterModeArgNames.length+1) {
			return 0;
		}
		CommandBuilder cmdBuilder = cmdBuilder(enterModeCmdClass);
		for(int i = 0; i < enterModeArgNames.length; i++) {
			cmdBuilder.set(enterModeArgNames[i], words[i+1]);
			used++;
		}
		cmdBuilder.execute();
		return used;
	}
	
	public class ModeCloser implements AutoCloseable {
		@Override
		public void close() {
			popCommandMode();
		}
	}
	
	public XPath getXpathEngine() {
		return xpathEngine;
	}


	public CommandMode<?> popCommandMode() {
		CommandMode<?> commandMode = commandModeStack.remove(0);
		if(commandMode instanceof DbContextChangingMode) {
			objectContextStack.remove(0);
			uncommittedCache.clear();
		}
		commandMode.exit();
		return commandMode;
	}
	
	public CommandMode<?> peekCommandMode() {
		if(commandModeStack.isEmpty()) {
			return null;
		}
		return commandModeStack.get(0);
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
		uncommittedCache.clear();
		getObjectContext().commitChanges();
	}
	
	public void newObjectContext() {
		objectContextStack.remove(0);
		uncommittedCache.clear();
		objectContextStack.add(0, peekCommandMode().getServerRuntime().getContext());
	}

	public <R extends CommandResult, C extends Command<R>> CommandBuilder<R, C> cmdBuilder(Class<C> cmdClass) {
		return new CommandBuilder<R, C>(this, cmdClass);
	}

	@SuppressWarnings("rawtypes")
	public void checkCommmandIsExecutable(Class<? extends Command> cmdClass) {
	}

	public String getProjectSettingValue(ProjectSettingOption projectSettingOption) {
		ProjectSetting projectSetting = 
				GlueDataObject.lookup(this, ProjectSetting.class, 
						ProjectSetting.pkMap(projectSettingOption.name()), true);
		String valueText;
		if(projectSetting == null) {
			valueText = projectSettingOption.getDefaultValue();
		} else {
			valueText = projectSetting.getValue();
		}
		return valueText;
	}
	
	public void cacheUncommitted(GlueDataObject dataObject) {
		uncommittedCache.put(new CacheKey(dataObject.getClass(), dataObject.pkMap()), dataObject);
	}

	@SuppressWarnings("unchecked")
	public <C extends GlueDataObject> C lookupUncommitted(Class<C> dataObjectClass, Map<String, String> pkMap) {
		return (C) uncommittedCache.get(new CacheKey(dataObjectClass, pkMap));
	}

	private static class CacheKey {
		private Class<? extends GlueDataObject> dataObjectClass;
		private Map<String, String> pkMap;
		
		public CacheKey(Class<? extends GlueDataObject> dataObjectClass,
				Map<String, String> pkMap) {
			super();
			this.dataObjectClass = dataObjectClass;
			this.pkMap = pkMap;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((dataObjectClass == null) ? 0 : dataObjectClass
							.hashCode());
			result = prime * result + ((pkMap == null) ? 0 : pkMap.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (dataObjectClass == null) {
				if (other.dataObjectClass != null)
					return false;
			} else if (!dataObjectClass.equals(other.dataObjectClass))
				return false;
			if (pkMap == null) {
				if (other.pkMap != null)
					return false;
			} else if (!pkMap.equals(other.pkMap))
				return false;
			return true;
		}
	}
	
	public void dispose() {
		while(!(peekCommandMode() instanceof RootCommandMode)) {
			popCommandMode();
		}
	}

	public NashornContext getNashornContext() {
		if(nashornContext == null) {
			this.nashornContext = new NashornContext(this);
			this.nashornContext.init();
		}
		return nashornContext;
	}

	
	
	
}
