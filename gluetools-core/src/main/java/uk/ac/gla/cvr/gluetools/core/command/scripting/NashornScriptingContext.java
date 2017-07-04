package uk.ac.gla.cvr.gluetools.core.command.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.json.JsonObject;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.scripting.NashornScriptingException.Code;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentJsonUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public class NashornScriptingContext {

	private CommandContext cmdContext;

	public NashornScriptingContext(CommandContext cmdContext) {
		super();
		this.cmdContext = cmdContext;
	}

	public void runScript(String filePath, String scriptContent) {
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		ScriptEngine engine = factory.getScriptEngine(this.getClass().getClassLoader());
		ScriptContext scriptContext = engine.getContext();
		Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put("glueAux", new GlueBinding()); // accessed from glue.js
		loadSource(engine, classpathSourceMap("glue.js"));
		loadSource(engine, classpathSourceMap("underscore.js"));
		try {
			loadSource(engine, sourceMap(filePath, scriptContent));
		} catch (NashornException e) {
			recastException(filePath, e);
		}
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
			cmdContext.pushCommandMode(modePath.replaceFirst("/", "").split("/"));
		}
		
		public void popMode() {
			cmdContext.popCommandMode();
		}

		public String currentMode() {
			return cmdContext.getModePath();
		}

		@SuppressWarnings("rawtypes")
		public Map command(ScriptObjectMirror scrObjMirror) {
			CommandResult cmdResult;
			try {
				CommandDocument commandDocument = ScriptObjectMirrorUtils.scriptObjectMirrorToCommandDocument(scrObjMirror);
				Document cmdXmlDocument = CommandDocumentXmlUtils.commandDocumentToXmlDocument(commandDocument);
				Element cmdDocElem = cmdXmlDocument.getDocumentElement();
				@SuppressWarnings("unused")
				Class<? extends Command> cmdClass = cmdContext.commandClassFromElement(cmdDocElem);
				// TODO check cmdClass.
				Command command = cmdContext.commandFromElement(cmdDocElem);
				if(command == null) {
					// Nashorn-friendly exception
					JsonObject jsonObject = CommandDocumentJsonUtils.commandDocumentToJsonObject(commandDocument);
					String modePath = cmdContext.getModePath();
					throw new NashornScriptingException(Code.UNKNOWN_COMMAND, JsonUtils.print(jsonObject, false), modePath);
				}
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
		InputStream utilInputStream = NashornScriptingContext.class.getResourceAsStream("/nashornJS/"+sourceFileName);
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
	
    private void loadSource(ScriptEngine e, Map<String,String> sourceMap) {
		// Get original load function
		final JSObject loadFn = (JSObject)e.get("load");
		// Get global. Not really necessary as we could use null too, just for
		// completeness.
		final JSObject thiz;
		try {
			thiz = (JSObject)e.eval("(function() { return this; })()");
		} catch(ScriptException se) {
			throw new RuntimeException(se);
		}
		loadFn.call(thiz, sourceMap);
    }
	
	
	
	
}
