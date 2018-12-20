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
package uk.ac.gla.cvr.gluetools.core.command.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.scripting.NashornScriptingException.Code;
import uk.ac.gla.cvr.gluetools.core.console.Console;
import uk.ac.gla.cvr.gluetools.core.console.Lexer;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.Token;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.session.SessionKey;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentJsonUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public class NashornContext {

	private CommandContext cmdContext;
	private NashornScriptEngineFactory engineFactory;
	private ScriptEngine engine;
	private List<CompiledScript> compiledScripts = new ArrayList<CompiledScript>();

	public NashornContext(CommandContext cmdContext) {
		super();
		this.cmdContext = cmdContext;
	}

	public void runScript(String filePath, String scriptContent) {
		// create new context for this run, ensuring that any globals defined
		// in the script are local to this run.
		ScriptContext scriptContext = newScriptContext();
		loadScriptInContext(scriptContext, filePath, scriptContent);
	}

	public void loadScriptInContext(ScriptContext context, String scriptFilePath, String scriptContent) {
		engine.setContext(context);
		try {
			// use loadSource here to ensure the file path is associated with the script.
			// this means it will come up during exceptions.
			loadSource(sourceMap(scriptFilePath, scriptContent));
		} catch (NashornException e) {
			recastException(scriptFilePath, e);
		}
	}

	public ScriptContext newScriptContext() {
		ScriptContext context = new SimpleScriptContext();
		Bindings bindings = engine.createBindings();
		bindings.put("glueAux", new GlueBinding()); // Java context, accessed from glue.js
		context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
		// ensure compiled scripts are evaluated in this context.
		try {
			for(CompiledScript compiledScript: compiledScripts) {
				compiledScript.eval(context);
			}
		} catch(ScriptException se) {
			throw new RuntimeException(se);
		}
		return context;
	}

	public void recastException(String filePath, NashornException ex) {
		String jsStackTrace = NashornException.getScriptStackString(ex);
		String message = ex.getMessage();
		if(message.startsWith("Error: ")) {
			message = message.substring("Error: ".length());
		}
		Object ecmaError = ex.getEcmaError();
		Throwable javaEx = null;
		if(ecmaError instanceof ScriptObjectMirror) {
			Object javaExObj = ((ScriptObjectMirror) ecmaError).get("javaEx");
			if(javaExObj instanceof Throwable) {
				javaEx = (Throwable) javaExObj;
			}
		}
		throw new NashornScriptingException(javaEx, Code.SCRIPT_EXCEPTION, ex.getFileName(), ex.getLineNumber(), ex.getColumnNumber(), message, jsStackTrace);
	}

	public class GlueBinding {
		public void log(String level, String message) {
			LogRecord logRecord = new LogRecord(Level.parse(level), message);
			logRecord.setSourceClassName("NashornJsScript");
			GlueLogger.getGlueLogger().log(logRecord);
		}
		
		public void pushMode(String modePath) {
			if(modePath.startsWith("/")) {
				modePath = modePath.substring(1);
			}
			List<String> words = Arrays.asList(modePath.split("/"));
			int numWordsUsed = 0;
			while(numWordsUsed < words.size()) {
				String[] nextWords = words.subList(numWordsUsed, words.size()).toArray(new String[]{});
				int nextWordsUsed = cmdContext.pushCommandModeReturnNumWordsUsed(nextWords);
				if(nextWordsUsed == 0) {
					break;
				}
				numWordsUsed += nextWordsUsed;
			}
			if(numWordsUsed != words.size()) {
				throw new NashornScriptingException(Code.MALFORMED_MODE_PATH, modePath);
			}
		}
		
		public void popMode() {
			cmdContext.popCommandMode();
		}

		public String currentMode() {
			return cmdContext.getModePath();
		}

		public void initSession(String sessionType, ScriptObjectMirror sessionArgsObj) {
			String[] sessionArgs = sessionArgsFromScriptObjectMirror(sessionArgsObj);
			SessionKey sessionKey = new SessionKey(sessionType, sessionArgs);
			cmdContext.initSession(sessionKey);
		}

		public void closeSession(String sessionType, ScriptObjectMirror sessionArgsObj) {
			String[] sessionArgs = sessionArgsFromScriptObjectMirror(sessionArgsObj);
			SessionKey sessionKey = new SessionKey(sessionType, sessionArgs);
			cmdContext.closeSession(sessionKey);
		}

		
		@SuppressWarnings("rawtypes")
		public Map runCommandFromObject(ScriptObjectMirror scrObjMirror) {
			CommandDocument commandDocument;
			try {
				commandDocument = ScriptObjectMirrorUtils.scriptObjectMirrorToCommandDocument(scrObjMirror);
			} catch(Exception e) {
				throw new NashornScriptingException(e, Code.COMMAND_INPUT_ERROR, "Unable to convert JavaScript object to input command document: "+e.getMessage());
			}
			Document cmdXmlDocument = CommandDocumentXmlUtils.commandDocumentToXmlDocument(commandDocument);
			Element cmdDocElem = cmdXmlDocument.getDocumentElement();
			@SuppressWarnings("unused")
			Class<? extends Command> cmdClass = cmdContext.commandClassFromElement(cmdDocElem);
			if(cmdClass == null) {
				// Nashorn-friendly exception
				JsonObject jsonObject = CommandDocumentJsonUtils.commandDocumentToJsonObject(commandDocument);
				String modePath = cmdContext.getModePath();
				throw new NashornScriptingException(Code.UNKNOWN_COMMAND, JsonUtils.print(jsonObject, false), modePath);
			}
			cmdContext.checkCommmandIsExecutable(cmdClass);
			Command command = cmdContext.commandFromElement(cmdDocElem);
			return runCommand(command);
		}

		@SuppressWarnings("rawtypes")
		public Map runCommandFromString(String string) {
			List<Token> tokens = Lexer.lex(string);
			List<Token> meaningfulTokens = Lexer.meaningfulTokens(tokens);
			List<String> tokenStrings = meaningfulTokens.stream().map(t -> t.render()).collect(Collectors.toList());
			return runCommandFromList(tokenStrings);
		}

		@SuppressWarnings("rawtypes")
		private Map runCommandFromList(List<String> tokenStrings) {
			CommandFactory commandFactory = cmdContext.peekCommandMode().getCommandFactory();
			Class<? extends Command> commandClass = commandFactory.identifyCommandClass(cmdContext, tokenStrings);
			if(commandClass == null) {
				throw new CommandException(CommandException.Code.UNKNOWN_COMMAND, String.join(" ", tokenStrings), cmdContext.getModePath());
			}
			if(commandClass.getAnnotation(EnterModeCommandClass.class) != null || 
					commandClass.getSimpleName().equals("ExitCommand") ||
					commandClass.getSimpleName().equals("QuitCommand")) {
				throw new NashornScriptingException(Code.COMMAND_INPUT_ERROR, "Mode changing commands cannot be run from JavaScript, use the mode changing utility functions instead");
			}
			String[] commandWords = CommandUsage.cmdWordsForCmdClass(commandClass);
			LinkedList<String> argStrings = new LinkedList<String>(tokenStrings.subList(commandWords.length, tokenStrings.size()));
			cmdContext.checkCommmandIsExecutable(commandClass);
			if(CommandUsage.hasMetaTagForCmdClass(commandClass, CmdMeta.inputIsComplex)) {
				String cmdWords = String.join(" ", CommandUsage.cmdWordsForCmdClass(commandClass));
				throw new NashornScriptingException(Code.COMMAND_INPUT_ERROR, "Input must be in the form of an object / document for command \""+cmdWords+"\"");
			}
			Map<String, Object> docoptMap;
			String docoptUsageSingleWord = CommandUsage.docoptStringForCmdClass(commandClass, true);
			docoptMap = Console.runDocopt(commandClass, docoptUsageSingleWord, argStrings);
			Command command = Console.buildCommand(cmdContext, commandClass, docoptMap);
			return runCommand(command);
		}

		@SuppressWarnings("rawtypes")
		public Map runCommandFromArray(ScriptObjectMirror scrObjMirror) {
			List<String> tokenStrings = new ArrayList<String>();
			for(Object obj: scrObjMirror.values()) {
				String string;
				if(obj instanceof String) {
					string = (String) obj;
				} else if(obj instanceof Number) {
					// javascript does not have integers, only floats
					// here we force integer if the number is mathematically an integer.
					Number num = (Number) obj;
					double doubleVal = Math.round(num.doubleValue());
					if(doubleVal == num.doubleValue()) {
						string = Integer.toString(num.intValue());
					} else {
						string = obj.toString();
					}
				} else if(obj instanceof Boolean) {
					string = obj.toString();
				} else if(obj == null) {
					throw new NashornScriptingException(Code.COMMAND_INPUT_ERROR, "Null values may not be used in command input array");
				} else {
					throw new NashornScriptingException(Code.COMMAND_INPUT_ERROR, "Values in command input array must be strings, numbers or booleans");
				}
				tokenStrings.add(string);
			}
			return runCommandFromList(tokenStrings);
		}

		@SuppressWarnings("rawtypes")
		private Map runCommand(Command command) {
			CommandResult cmdResult;
			try {
				cmdResult = command.execute(cmdContext);
			} catch(Exception e) {
				GlueLogger.getGlueLogger().log(Level.FINEST, e, () -> "GLUE command invoked from NashornScriptingContext threw an exception: ");
				throw e;
			} 
			CommandDocumentToMapVisitor cmdDocToMapVisitor = new CommandDocumentToMapVisitor();
			cmdResult.getCommandDocument().accept(cmdDocToMapVisitor);
			return cmdDocToMapVisitor.getRootMap();
		}
	};

	private Map<String, String> classpathSourceMap(String sourceFileName) {
		InputStream utilInputStream = NashornContext.class.getResourceAsStream("/nashornJS/"+sourceFileName);
		String jsSource;
		try {
			 jsSource = IOUtils.toString(utilInputStream);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
        return sourceMap("glueEngine:"+sourceFileName, jsSource);
	}

	private Map<String, String> sourceMap(String sourceName, String jsSource) {
		final Map<String, String> sourceMap = new LinkedHashMap<String, String>();
		sourceMap.put("name", sourceName);
        sourceMap.put("script", jsSource);
        return sourceMap;
	}
	
    private void loadSource(Map<String,String> sourceMap) {
		JSObject loadFn = lookupFunction("load");
		invokeFunction(loadFn, sourceMap);
    }

    public Object invokeFunction(JSObject function, Object ... args) {
		// Get global. Not really necessary as we could use null too, just for
		// completeness.
		final JSObject thiz;
		try {
			thiz = (JSObject) (engine.eval("(function() { return this; })()"));
		} catch(ScriptException se) {
			throw new RuntimeException(se);
		}
		return function.call(thiz, args);
    }
    
	public void init() {
		this.engineFactory = new NashornScriptEngineFactory();
		this.engine = engineFactory.getScriptEngine(this.getClass().getClassLoader());
		compiledScripts.add(compileFromClasspath("glue.js"));
		compiledScripts.add(compileFromClasspath("underscore.js"));
	}
	
	private CompiledScript compileFromClasspath(String fileName) {
		String source = classpathSourceMap(fileName).get("script");
		try {
			CompiledScript compiledScript = ((Compilable) engine).compile(source);
			return compiledScript;
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
	}

	public void setScriptContext(ScriptContext scriptContext) {
		this.engine.setContext(scriptContext);
	}

	public JSObject lookupFunction(String functionName) {
		Object fnObj = engine.get(functionName);
		if(fnObj == null) {
			return null;
		}
		if(!(fnObj instanceof JSObject)) {
			throw new NashornScriptingException(Code.SCRIPT_EXCEPTION, "Value "+functionName+" is not a JSObject");
		}
		return ((JSObject) fnObj);
	}

	public void setupConfigDocument(ScriptContext scriptContext, String name, CommandDocument configDocument) {
		CommandDocumentToMapVisitor cmdDocumentToMapVisitor = new CommandDocumentToMapVisitor();
		configDocument.accept(cmdDocumentToMapVisitor);
		JSObject valueToNativeFn = lookupFunction("valueToNative");
		Map<String, Object> rootMap = cmdDocumentToMapVisitor.getRootMap();
		Object configDocumentObj = rootMap.get("configDocument");
		Object nativeCfgDocObj = invokeFunction(valueToNativeFn, configDocumentObj);
		scriptContext.setAttribute(name, nativeCfgDocObj, ScriptContext.ENGINE_SCOPE);
	
	}
	
	private String[] sessionArgsFromScriptObjectMirror(ScriptObjectMirror sessionArgsObj) {
		List<String> sessionArgs = new ArrayList<String>();
		for(Object obj: sessionArgsObj.values()) {
			String sessionArg;
			if(obj instanceof String) {
				sessionArg = (String) obj;
			} else if(obj instanceof Number) {
				// javascript does not have integers, only floats
				// here we force integer if the number is mathematically an integer.
				Number num = (Number) obj;
				double doubleVal = Math.round(num.doubleValue());
				if(doubleVal == num.doubleValue()) {
					sessionArg = Integer.toString(num.intValue());
				} else {
					sessionArg = obj.toString();
				}
			} else if(obj instanceof Boolean) {
				sessionArg = obj.toString();
			} else if(obj == null) {
				sessionArg = null;
			} else {
				throw new NashornScriptingException(Code.COMMAND_INPUT_ERROR, "Values in sessionArgs array must be strings, numbers, booleans or nulls");
			}
			sessionArgs.add(sessionArg);
		}
		return sessionArgs.toArray(new String[] {});
	}

}
