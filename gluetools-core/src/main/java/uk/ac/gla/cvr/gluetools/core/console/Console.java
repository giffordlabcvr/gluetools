package uk.ac.gla.cvr.gluetools.core.console;

import java.io.IOException;
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

public class Console implements CommandContextListener
{
	private PrintWriter out;
	private ConsoleReader reader;
	private Completer currentCompleter;
	private ConsoleCommandContext commandContext;
	private GluetoolsEngine gluetoolsEngine;
	
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

	private void handleNextLine() throws IOException {
		String line = reader.readLine();
		ArrayList<Token> tokens = null;
		CommandResult commandResult = null;
		try {
			tokens = Lexer.lex(line);
			// output(tokens.toString());
			commandResult = tokensToCommandResult(tokens);
			
		} catch(GlueException ge) {
			handleGlueException(ge);
			return;
		}
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
				int minColWidth = 15;
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
						row.addCellValue(c);
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
		while(!console.isFinished()) {
			console.handleNextLine();
		}
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
			outputError(glueException.getLocalizedMessage());
		}
	}
	
	private void output(String message) {
		out.println(message);
		out.flush();
	}

	private void outputError(String message) {
		output("Error: "+message);
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
