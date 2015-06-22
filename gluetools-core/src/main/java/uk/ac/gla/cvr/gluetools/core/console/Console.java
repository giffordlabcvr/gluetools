package uk.ac.gla.cvr.gluetools.core.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.apache.cayenne.ObjectId;
import org.apache.commons.lang.StringUtils;
import org.docopt.Docopt;
import org.docopt.DocoptExitException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.GlueException.GlueErrorCode;
import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContextListener;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.Token;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.TokenType;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

import com.brsanthu.dataexporter.model.AlignType;
import com.brsanthu.dataexporter.model.Row;
import com.brsanthu.dataexporter.model.StringColumn;
import com.brsanthu.dataexporter.output.texttable.TextTableExportOptions;
import com.brsanthu.dataexporter.output.texttable.TextTableExportStyle;
import com.brsanthu.dataexporter.output.texttable.TextTableExporter;

// TODO accept on the command line an XML configuration file to configure the DB connection and other items.
// TODO allow configuration via a System property.
// TODO command lines ending with '\' should be concatenated to allow continuations.
// TODO it should be possible to prefix mode-changing commands and so execute them temporarily in that mode.
// TODO completion should extend to arguments, at least when these arguments select from a table / enum / map.
// TODO improve command line table display to adapt columns to data.
// TODO implement paging of commands in interactive mode.
// TODO implement toggle for verbose exception reporting.
// TODO catch general exceptions and continue.
// TODO emit warning about temporary database
// TODO implement toggle for whether output should be echoed in batch mode.
// TODO history should be stored in the DB.
// TODO in batch mode, exceptions should go to stderr.
public class Console implements CommandContextListener
{
	private PrintWriter out;
	private ConsoleReader reader;
	private Completer currentCompleter;
	private ConsoleCommandContext commandContext;
	private GluetoolsEngine gluetoolsEngine;
	private Integer batchLine = null;
	private boolean verboseError = false;
	
	private Console() throws IOException {
		this.reader = new ConsoleReader();
		this.out = new PrintWriter(reader.getOutput());
		this.gluetoolsEngine = GluetoolsEngine.getInstance();
		this.commandContext = new ConsoleCommandContext(gluetoolsEngine);
		commandContext.setCommandContextListener(this);
		commandContext.pushCommandMode(CommandMode.ROOT);
	}
	
	private boolean isFinished() {
		return commandContext.isFinished();
	}

	private void handleInteractiveLine() throws IOException {
		String line = reader.readLine();
		try {
			handleLine(line);
		} catch(GlueException ge) {
			handleGlueException(ge);
		}
	}

	private void handleLine(String line) {
		ArrayList<Token> tokens = null;
		CommandResult commandResult = null;
		tokens = Lexer.lex(line);
		// output(tokens.toString());
		commandResult = tokensToCommandResult(tokens);
		renderCommandResult(commandResult);
	}

	private void renderCommandResult(CommandResult commandResult) {
		if(commandResult == null) { return; }
		if(commandResult == CommandResult.OK) {
			output("OK");
		} else if(commandResult instanceof CreateCommandResult) {
			CreateCommandResult objCreateResult = (CreateCommandResult) commandResult;
			ObjectId objectId = objCreateResult.getObjectId();
			output(objectId.getEntityName()+" created: "+objectId.getIdSnapshot().toString());
		} else if(commandResult instanceof DocumentResult) {
			Document document = ((DocumentResult) commandResult).getDocument();
			byte[] docBytes = XmlUtils.prettyPrint(document);
			// TODO -- what encoding? -- maybe prettyPrint should have a parameter encoding?
			output(new String(docBytes));			
		} else if(commandResult instanceof ListCommandResult<?>) {
			StringWriter stringWriter = new StringWriter();
			ListCommandResult<?> listCmdResult = (ListCommandResult<?>) commandResult;
			List<? extends GlueDataObject> results = listCmdResult.getResults();
			int numFound = results.size();
			if(numFound > 0) {
				final int minColWidth = 25;
				String[] headers = listCmdResult.getResultClass().getAnnotation(GlueDataClass.class).listColumnHeaders();
				TextTableExportOptions options = new TextTableExportOptions();
				options.setStyle(TextTableExportStyle.CLASSIC);
				TextTableExporter textTable = new TextTableExporter(stringWriter);
				for(String header: headers) {
					StringColumn column = new StringColumn(header, Math.max(minColWidth, header.length()));
					column.setAlign(AlignType.TOP_LEFT);
					textTable.addColumns(column);
				}
				for(GlueDataObject glueDataObj: results) {
					Row row = new Row();
					Arrays.asList(glueDataObj.populateListRow()).forEach(c -> {
						String cString = c.toString();
						if(cString.length() < minColWidth) {
							cString = " "+cString;
						}
						row.addCellValue(cString);
					});
					textTable.addRows(row);
				}
				textTable.finishExporting();
			} 
			output(stringWriter.toString()+"Number found: "+numFound);
		} else if(commandResult instanceof ConsoleCommandResult) {
			output(((ConsoleCommandResult) commandResult).getResultAsConsoleText());
		}
	}


	private CommandResult tokensToCommandResult(List<Token> tokens) {
		List<Token> nonWSTokens = tokens.stream().filter(t -> t.getType() != TokenType.WHITESPACE).collect(Collectors.toList());
		if(nonWSTokens.isEmpty()) {
			return null;
		}
		Token firstToken = nonWSTokens.get(0);
		String identifier = firstToken.render();
		CommandFactory commandFactory = commandContext.peekCommandMode().getCommandFactory();
		Class<? extends Command> commandClass = commandFactory.classForElementName(identifier);
		if(commandClass == null) {
			throw new ConsoleException(Code.UNKNOWN_COMMAND, identifier, commandContext.getModePath());
		}
		
		String docoptUsage = CommandUsage.docoptStringForCmdClass(commandClass);
		List<String> argStrings = nonWSTokens.subList(1, nonWSTokens.size()).stream().
				map(t -> t.render()).collect(Collectors.toList());
		Map<String, Object> docoptMap;
		try {
			docoptMap = new Docopt(docoptUsage).withHelp(false).withExit(false).parse(argStrings);
		} catch(DocoptExitException dee) {
			throw new ConsoleException(dee, Code.COMMAND_USAGE_ERROR, docoptUsage.trim());
		}
		Element element = elementFromDocoptMap(identifier, docoptMap);
		Command command;
		try {
			command = commandContext.commandFromElement(element);
		} catch(PluginConfigException pce) {
			if(pce.getCode() == PluginConfigException.Code.CONFIG_FORMAT_ERROR &&
					pce.getErrorArgs().length >= 3 && 
					pce.getErrorArgs()[0].toString().endsWith("/text()")) {
				throw new ConsoleException(pce, Code.ARGUMENT_FORMAT_ERROR,
						pce.getErrorArgs()[0].toString().replace("/text()", ""),
						pce.getErrorArgs()[1], pce.getErrorArgs()[2]);
			} else {
				throw pce;
			}
		}
		return command.execute(commandContext);
	}

	private Element elementFromDocoptMap(String identifier, Map<String, Object> docoptMap) {
		Document document = XmlUtils.newDocument();
		Element rootElem = (Element) document.appendChild(document.createElement(identifier));
		docoptMap.forEach((key, value) -> {
			if(value == null) { return; }
			String tagName;
			if(key.startsWith("<") && key.endsWith(">")) {
				tagName = key.substring(1, key.length()-1);
			} else if(key.startsWith("--")) {
				tagName = key.substring(2);
			} else if(key.startsWith("-")) {
				tagName = key.substring(1);
			} else {
				tagName = key;
			}
			if(value instanceof Collection<?>) {
				((Collection <?>) value).forEach(item -> {
					Element itemElem = (Element) rootElem.appendChild(document.createElement(tagName));
					itemElem.appendChild(document.createTextNode(item.toString()));
				});
			} else {
				Element valueElem = (Element) rootElem.appendChild(document.createElement(tagName));
				valueElem.appendChild(document.createTextNode(value.toString()));
			}
		});
		return rootElem;
	}

	public static void main(String[] args) throws IOException {
		Console console = new Console();
		Docopt docopt = null;
		try(InputStream docoptStream = Console.class.getResourceAsStream("/Console.docopt")) {
			docopt = new Docopt(docoptStream);
		} 
		Map<String, Object> docoptResult = docopt.parse(args);
		Object verboseErrorOption = docoptResult.get("--verbose-error");
		if(verboseErrorOption != null) {
			console.verboseError = Boolean.parseBoolean(verboseErrorOption.toString());
		}
		Object fileString = docoptResult.get("--file");
		if(fileString != null) {
			console.runBatchFile(fileString);
		} else {
			while(!console.isFinished()) {
				console.handleInteractiveLine();
			}
		}
	}

	private void runBatchFile(Object fileString) {
		String fileContent = null;
		try {
			fileContent = new String(commandContext.loadBytes(fileString.toString()));
		} catch(GlueException glueException) {
			handleGlueException(glueException);
			System.exit(1);
		}
		String[] fileCommands = fileContent.split("\n");
		int batchLine = 1;
		for(String fileCommandLine: fileCommands) {
			setBatchLine(batchLine);
			try {
				handleLine(fileCommandLine);
			} catch(GlueException ge) {
				handleGlueException(ge);
				System.exit(1);
			}
			batchLine++;
			
		}
	}


	private Integer getBatchLine() {
		return batchLine;
	}

	private void setBatchLine(Integer batchLine) {
		this.batchLine = batchLine;
	}

	private void handleGlueException(GlueException glueException) {
		GlueErrorCode errorCode = glueException.getCode();
		Object[] errorArgs = glueException.getErrorArgs();
		if(glueException instanceof ConsoleException && errorCode == Code.SYNTAX_ERROR && 
				errorArgs.length >= 1 && errorArgs[0] instanceof Integer) {
			int position = (Integer) errorArgs[0];
			output(StringUtils.repeat(" ", position+reader.getPrompt().length())+"^");
			outputError("Bad syntax");
		} else {
			outputError(glueException);
		}
	}
	
	private void outputError(Throwable exception) {
		outputError(exception.getLocalizedMessage());
		if(verboseError) {
			outputStackTrace(exception);
		}
		Throwable cause = exception.getCause();
		while(cause != null && cause != exception) {
			output("Cause: "+cause.getLocalizedMessage());
			if(verboseError) {
				outputStackTrace(cause);
			}
			exception = cause;
			cause = cause.getCause();
		}
	}

	private void outputStackTrace(Throwable exception) {
		output(exception.getClass().getCanonicalName());
		for(StackTraceElement ste: exception.getStackTrace()) {
			output(ste.toString());
		}
	}

	private void output(String message) {
		out.println(message);
		out.flush();
	}

	private void outputError(String message) {
		if(getBatchLine() == null) {
			output("Error: "+message);
		} else {
			output("Error at line "+getBatchLine()+": "+message);
		}
	}

	private void updatePrompt() {
		reader.setPrompt(commandContext.getModePath()+"> ");
	}

	@Override
	public void commandModeChanged() {
		updatePrompt();
		updateCompleter();
	}

	private void updateCompleter() {
		List<String> commands = new ArrayList<String>(commandContext.peekCommandMode().getCommandFactory().getElementNames());
		Collections.sort(commands);
		if(currentCompleter != null) {
			reader.removeCompleter(currentCompleter);
		}
		currentCompleter = new StringsCompleter(commands);
		reader.addCompleter(currentCompleter);
	}
	
	
}
