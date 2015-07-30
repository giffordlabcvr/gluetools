package uk.ac.gla.cvr.gluetools.ws;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandFormatUtils;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandDescriptor;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class WsCmdContext extends CommandContext {

	public WsCmdContext(GluetoolsEngine gluetoolsEngine) {
		super(gluetoolsEngine);
		ServerRuntime rootServerRuntime = gluetoolsEngine.getRootServerRuntime();
		RootCommandMode rootCommandMode = new RootCommandMode(rootServerRuntime);
		pushCommandMode(rootCommandMode);
	}
	
	private String enterModeCommandWord = null;
	private List<String> enterModeCommandArgs = new LinkedList<String>();
	private Class<? extends Command> enterModeCommandClass;
	private String fullPath = "/";
	
	
	// TODO if a GET is executed when command word is a single word with no arguments, try a list command with that word?
	/*@GET()
	@Produces(MediaType.APPLICATION_JSON)
	public String getAsList() {
	}*/

	@POST()
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String postAsCommand(String commandString) {
		Element cmdDocElem = CommandFormatUtils.cmdDocElemFromJsonString(commandString);
		Command command = commandFromElement(cmdDocElem);
		if(command == null) {
			throw new CommandException(CommandException.Code.UNKNOWN_COMMAND, commandString, fullPath);
		}
		return command.execute(this).getJsonObject().toString();
	}
	
	// sub mode URL navigation
	@Path("/{urlPathSegment}")
	public Object handle(@PathParam("urlPathSegment") String urlPathSegment) {
		if(fullPath.equals("/")) {
			fullPath = fullPath+urlPathSegment;
		} else {
			fullPath = fullPath+"/"+urlPathSegment;
		}
		if(enterModeCommandWord == null) {
			CommandMode<?> commandMode = peekCommandMode();
			Class<? extends Command> cmdClass = commandMode.getCommandFactory().identifyCommandClass(this, Collections.singletonList(urlPathSegment));
			if(cmdClass.getAnnotation(EnterModeCommandClass.class) != null) {
				enterModeCommandWord = urlPathSegment;
				enterModeCommandClass = cmdClass;
				return this;
			} else {
				throw new NotFoundException();
			}
		} else {
			EnterModeCommandDescriptor enterModeCommandDescriptor = EnterModeCommandDescriptor.getDescriptorForClass(enterModeCommandClass);
			String[] enterModeArgNames = enterModeCommandDescriptor.enterModeArgNames();
			if(enterModeCommandArgs.size() < enterModeArgNames.length) {
				enterModeCommandArgs.add(urlPathSegment);
			}
			if(enterModeCommandArgs.size() == enterModeArgNames.length) {
				Element cmdElem = GlueXmlUtils.documentWithElement(enterModeCommandWord);
				for(int i = 0; i < enterModeArgNames.length; i++) {
					GlueXmlUtils.appendElementWithText(cmdElem, enterModeArgNames[i], enterModeCommandArgs.get(i));
				}
				enterModeCommandArgs.clear();
				enterModeCommandWord = null;
				enterModeCommandClass = null;
				// run enter mode command
				Command enterModeCommand = commandFromElement(cmdElem);
				try {
					enterModeCommand.execute(this);
				} catch(DataModelException dme) {
					if(dme.getCode() == DataModelException.Code.OBJECT_NOT_FOUND) {
						throw new NotFoundException(dme);
					}
				}
				return this;
			} else {
				return this;
			}
		}
		
	}
	
	
	
}
