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

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@SuppressWarnings("rawtypes")
public abstract class CommandMode<C extends Command> {
	
	public static String CORE_DOMAIN_RESOURCE = "cayenne-gluecore-domain.xml";
	public static String CORE_MAP_RESOURCE = "gluecore-map.map.xml";
	
	private CommandMode<?> parentCommandMode;

	
	// TODO need to enhance this so that Console commands are only available 
	// for a given mode when we are using the console.
	
	private String relativeModePath;
	
	private CommandFactory commandFactory;

	protected CommandMode(CommandFactory commandFactory, C enterModeCommand, String... modeIds) {
		setRelativeModePath(formRelativeModePath(enterModeCommand, modeIds));
		setCommandFactory(commandFactory);
	}

	protected CommandMode(C enterModeCommand, String... modeIds) {
		setRelativeModePath(formRelativeModePath(enterModeCommand, modeIds));
		setCommandFactory(CommandFactory.get(getClass().getAnnotation(CommandModeClass.class).commandFactoryClass()));
	}
	
	private static String formRelativeModePath(Command enterModeCommand, String... modeIds) {
		if(enterModeCommand == null) {
			return "/";
		}
		String firstCommandWord = CommandUsage.cmdWordsForCmdClass(enterModeCommand.getClass())[0];
		if(modeIds.length == 0) {
			return firstCommandWord+"/";
		}
		return firstCommandWord+"/"+String.join("/", modeIds)+"/";
	}
	
	public String getRelativeModePath() {
		return relativeModePath;
	}

	public CommandFactory getCommandFactory() {
		return commandFactory;
	}

	private void setRelativeModePath(String modeId) {
		this.relativeModePath = modeId;
	}

	private void setCommandFactory(CommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}


	@SuppressWarnings("rawtypes")
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass, Element elem) {
		
	}

	public ServerRuntime getServerRuntime() {
		CommandMode<?> parentCommandMode = getParentCommandMode();
		if(parentCommandMode != null) {
			return parentCommandMode.getServerRuntime();
		}
		return null;
	}

	protected CommandMode<?> getParentCommandMode() {
		return parentCommandMode;
	}

	void setParentCommandMode(CommandMode<?> parentCommandMode) {
		this.parentCommandMode = parentCommandMode;
	}

	public void exit() {
	}
	
	protected void appendModeConfigToElem(Element elem, String name, Object value) {
		Element newElem;
		if(value == null) {
			newElem = GlueXmlUtils.appendElement(elem, name);
			CommandDocumentXmlUtils.setGlueType(newElem, GlueTypeUtils.GlueType.Null, false);
		} else {
			newElem = (Element) GlueXmlUtils.appendElementWithText(elem, name, value.toString()).getParentNode();
			CommandDocumentXmlUtils.setGlueType(newElem, GlueTypeUtils.glueTypeFromObject(value), false);
		}
		newElem.setUserData("modeConfig", Boolean.TRUE, null);
	}


	
}
