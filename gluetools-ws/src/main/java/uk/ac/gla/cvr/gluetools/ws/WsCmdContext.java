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
package uk.ac.gla.cvr.gluetools.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandFormatUtils;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandDescriptor;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.requestGatekeeper.RequestFilterException;
import uk.ac.gla.cvr.gluetools.core.requestGatekeeper.RequestGatekeeper;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;
import uk.ac.gla.cvr.gluetools.core.requestQueue.RequestQueueManager;
import uk.ac.gla.cvr.gluetools.core.requestQueue.RequestStatus;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentJsonUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public class WsCmdContext extends CommandContext {

	public static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.ws");
	
	public WsCmdContext(GluetoolsEngine gluetoolsEngine) {
		super(gluetoolsEngine, "the GLUE web API");
		ServerRuntime rootServerRuntime = gluetoolsEngine.getRootServerRuntime();
		RootCommandMode rootCommandMode = new RootCommandMode(rootServerRuntime);
		pushCommandMode(rootCommandMode);
	}
	
	private List<String> enterModeCommandArgs = new LinkedList<String>();
	@SuppressWarnings("rawtypes")
	private Class<? extends Command> enterModeCommandClass;
	private String fullPath = "/";
	

	@POST()
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@SuppressWarnings("rawtypes")
	public String postAsCommand(@Context HttpHeaders requestHeaders, String commandString, @Context HttpServletResponse response) {
		GlueDataObject.resetTimeSpentInDbOperations();
		CommandDocument commandDocument = CommandFormatUtils.commandDocumentFromJsonString(commandString);
		Document cmdXmlDocument = CommandDocumentXmlUtils.commandDocumentToXmlDocument(commandDocument);
		Element cmdDocElem = cmdXmlDocument.getDocumentElement();
		Class<? extends Command> cmdClass = commandClassFromElement(cmdDocElem);
		if(cmdClass != null) {
			checkCommmandIsExecutable(cmdClass);
		}
		Command command = commandFromElement(cmdDocElem);
		if(command == null) {
			throw new CommandException(CommandException.Code.UNKNOWN_COMMAND, commandString, fullPath);
		}
		@SuppressWarnings("unused")
		long cmdExecutionStart = System.currentTimeMillis();
		String resultString;
		if(glueAsync(requestHeaders)) {
			RequestStatus requestStatus = invokeCommandAsync(commandString, command);
			resultString = requestStatus.toJsonString();
		} else {
			CommandResult cmdResult = invokeCommandSync(commandString, command);
			// logger.info("Time spent in database operations: "+(GlueDataObject.getTimeSpentInDbOperations())+"ms");
			//logger.info("Time spent in command execution: "+(System.currentTimeMillis() - cmdExecutionStart )+"ms");
			resultString = serializeToJson(cmdResult);
		}
		addCacheDisablingHeaders(response);
		return resultString;
	}
	
	@POST()
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public String postAsCommandMultipart(
			@Context HttpHeaders requestHeaders, 
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("command") String commandString, 
			@Context HttpServletResponse response) {
		return multipartCommand(requestHeaders, fileInputStream, commandString, response);
	}

	// for some reason IE preferentially accepts text/html on multipart requests,
	// so we go along with this. 
	// if not, it will prompt to save the JSON somewhere.
	@POST()
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_HTML)
	public String postAsCommandMultipartInternetExplorer(
			@Context HttpHeaders requestHeaders, 
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("command") String commandString, 
			@Context HttpServletResponse response) {
		return multipartCommand(requestHeaders, fileInputStream, commandString, response);
	}

	
	@SuppressWarnings({ "rawtypes" })
	private String multipartCommand(HttpHeaders requestHeaders, InputStream fileInputStream,
			String commandString, HttpServletResponse response) {
		GlueDataObject.resetTimeSpentInDbOperations();
		CommandDocument commandDocument = CommandFormatUtils.commandDocumentFromJsonString(commandString);
		Document commandXmlDocument = CommandDocumentXmlUtils.commandDocumentToXmlDocument(commandDocument);
		
		Element cmdDocElem = commandXmlDocument.getDocumentElement();
		Class<? extends Command> cmdClass = commandClassFromElement(cmdDocElem);
		if(cmdClass == null) {
			throw new CommandException(CommandException.Code.UNKNOWN_COMMAND, commandString, fullPath);
		}
		String[] cmdWords = CommandUsage.cmdWordsForCmdClass(cmdClass);
		if(!CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.consumesBinary)) {
			throw new CommandException(CommandException.Code.COMMAND_DOES_NOT_CONSUME_BINARY, 
					String.join(" ", cmdWords));
		}
		byte[] fileBytes;
		try {
			fileBytes = IOUtils.toByteArray(fileInputStream);
		} catch(IOException ioe) {
			throw new CommandException(ioe, CommandException.Code.COMMAND_BINARY_INPUT_IO_ERROR, cmdWords, ioe.getLocalizedMessage());
		}
		Element currentElem = cmdDocElem;
		for(int i = 1; i < cmdWords.length; i ++) {
			currentElem = GlueXmlUtils.findChildElements(currentElem, cmdWords[i]).get(0);
		}
		String fileBase64 = new String(Base64.getEncoder().encode(fileBytes));
		GlueXmlUtils.appendElementWithText(currentElem, Command.BINARY_INPUT_PROPERTY, fileBase64);
		Command command = commandFromElement(cmdDocElem);
		if(command == null) {
			throw new CommandException(CommandException.Code.UNKNOWN_COMMAND, commandString, fullPath);
		}
		@SuppressWarnings("unused")
		long cmdExecutionStart = System.currentTimeMillis();
		String resultString;
		if(glueAsync(requestHeaders)) {
			RequestStatus requestStatus = invokeCommandAsync(commandString, command);
			resultString = requestStatus.toJsonString();
		} else {
			CommandResult cmdResult = invokeCommandSync(commandString, command);
			// logger.info("Time spent in database operations: "+(GlueDataObject.getTimeSpentInDbOperations())+"ms");
			//logger.info("Time spent in command execution: "+(System.currentTimeMillis() - cmdExecutionStart )+"ms");
			resultString = serializeToJson(cmdResult);
		}

		addCacheDisablingHeaders(response);
		return resultString;
	}

	private boolean glueAsync(HttpHeaders requestHeaders) {
		List<String> glueAsyncHeaderVal = requestHeaders.getRequestHeader("glue-async");
		return glueAsyncHeaderVal != null && glueAsyncHeaderVal.size() > 0 && glueAsyncHeaderVal.get(0).equals("true");
	}
	
	public static String serializeToJson(CommandResult cmdResult) {
		@SuppressWarnings("unused")
		long jsonSerializationStart = System.currentTimeMillis();
		StringWriter stringWriter = new StringWriter();
		JsonGenerator jsonGenerator = JsonUtils.jsonGenerator(stringWriter);
		CommandDocumentJsonUtils.commandDocumentGenerateJson(jsonGenerator, cmdResult.getCommandDocument());
		jsonGenerator.flush();
		String cmdResultString = stringWriter.toString();
		//logger.info("Time spent in JSON serialization: "+(System.currentTimeMillis() - jsonSerializationStart)+"ms");
		return cmdResultString;
	}
	
	
	// sub mode URL navigation
	@Path("/{urlPathSegment}")
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object handle(@PathParam("urlPathSegment") String urlPathSegment) {
		if(fullPath.equals("/")) {
			fullPath = fullPath+urlPathSegment;
		} else {
			fullPath = fullPath+"/"+urlPathSegment;
		}
		if(enterModeCommandClass == null) {
			CommandMode<?> commandMode = peekCommandMode();
			Class<? extends Command> cmdClass = commandMode.getCommandFactory().identifyCommandClass(this, Collections.singletonList(urlPathSegment));
			if(cmdClass == null) {
				throw new CommandException(CommandException.Code.UNKNOWN_MODE_PATH, fullPath);
			}
			if(cmdClass.getAnnotation(EnterModeCommandClass.class) != null) {
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
				CommandBuilder cmdBuilder = cmdBuilder(enterModeCommandClass);
				for(int i = 0; i < enterModeArgNames.length; i++) {
					cmdBuilder.set(enterModeArgNames[i], enterModeCommandArgs.get(i));
				}
				enterModeCommandArgs.clear();
				enterModeCommandClass = null;
				// run enter mode command
				getGluetoolsEngine().runWithGlueClassloader(new Supplier<CommandResult>(){
					@Override
					public CommandResult get() {
						return cmdBuilder.execute();
					}
				});
				return this;
			} else {
				return this;
			}
		}
		
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void checkCommmandIsExecutable(Class<? extends Command> cmdClass) {
		super.checkCommmandIsExecutable(cmdClass);
		if(CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.consoleOnly)) {
			throw new CommandException(Code.NOT_EXECUTABLE_IN_CONTEXT, 
					String.join(" ", CommandUsage.cmdWordsForCmdClass(cmdClass)), 
					getDescription());
		}
	}
	
	private CommandResult invokeCommandSync(String commandString, Command<?> command) {
		RequestStatus requestStatus = invokeCommandAsync(commandString, command);
		RequestQueueManager requestQueueManager = getGluetoolsEngine().getRequestQueueManager();
		return requestQueueManager.collectRequestSync(requestStatus.getRequestID());
	}
	
	private RequestStatus invokeCommandAsync(String commandString, Command<?> command) {
		GluetoolsEngine gluetoolsEngine = getGluetoolsEngine();
		RequestGatekeeper requestGatekeeper = gluetoolsEngine.getRequestGatekeeper();
		String modePath = getModePath();
		Request request = new Request(modePath, command);
		boolean allowRequest = requestGatekeeper.allowRequest(request);
		if(!allowRequest) {
			String commandWords = String.join(" ", request.getCommandWords());
			String xmlDocString = new String(GlueXmlUtils.prettyPrint(request.getCommand().getCmdElem().getOwnerDocument()));
			throw new RequestFilterException(RequestFilterException.Code.REQUEST_DENIED, 
					commandWords,
					modePath,
					xmlDocString);
		}
		RequestQueueManager requestQueueManager = gluetoolsEngine.getRequestQueueManager();
		
		RequestStatus requestStatus = requestQueueManager.submitRequest(this, request);
		logger.info("Request ticket "+requestStatus.getRequestID()+" allocated to command : "+commandString);
		return requestStatus;
	}
	
	public static void addCacheDisablingHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setHeader("Expires", "0"); // Proxies.
	}

	@Override
	public synchronized boolean hasAuthorisation(String authorisationName) {
		return false;
	}

	@Override
	protected CommandContext createParallelWorkerInternal() {
		WsCmdContext parallelWorker = new WsCmdContext(getGluetoolsEngine());
		return parallelWorker;
	}
	
}
