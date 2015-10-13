package uk.ac.gla.cvr.gluetools.core.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jline.console.ConsoleReader;

import org.apache.commons.lang.StringUtils;
import org.docopt.Docopt;
import org.docopt.DocoptExitException;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.GlueException.GlueErrorCode;
import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContextListener;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandDescriptor;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.Token;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

// TODO allow configuration via a System property.
// TODO command lines ending with '\' should be concatenated to allow continuations.
// TODO completion should extend to arguments, at least when these arguments select from a table / enum / map.
// TODO improve command line table display to adapt columns to data.
// TODO implement paging of commands in interactive mode.
// TODO implement toggle for verbose exception reporting.
// TODO catch general exceptions and continue.
// TODO implement toggle for whether output should be echoed in batch mode.
// TODO history should be stored in the DB.
// TODO in batch mode, exceptions should go to stderr.
@SuppressWarnings("rawtypes")
public class Console implements CommandContextListener, CommandResultRenderingContext
{
	private static final String GLUE_PROMPT = "GLUE> ";
	private static final String GLUE_CONSOLE_BATCH_CONTEXT_STACK = "glue.console.batchContextStack";
	private PrintWriter out;
	private ConsoleReader reader;
	private ConsoleCommandContext commandContext;
	private LinkedList<BatchContext> batchContextStack = new LinkedList<BatchContext>();
	private String configFilePath = null;
	private boolean nonInteractive = true;
	private boolean migrateSchema;
	private boolean version;
	
	private Console() {
	}
	
	private boolean isFinished() {
		return commandContext.isFinished();
	}

	private void handleInteractiveLine() {
		String line = null;
		try {
			line = reader.readLine();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		try {
			handleLine(line, false, true);
		} catch(GlueException ge) {
			handleGlueException(ge);
		}
	}

	private void handleLine(String line, boolean outputCommandToConsole, boolean outputResultToConsole) {
		ArrayList<Token> tokens = null;
		tokens = Lexer.lex(line);
		// output(tokens.toString());
		List<Token> meaningfulTokens = Lexer.meaningfulTokens(tokens);
		if(!meaningfulTokens.isEmpty()) {
			List<String> tokenStrings = meaningfulTokens.stream().map(t -> t.render()).collect(Collectors.toList());
			if(outputCommandToConsole) {
				output(GLUE_PROMPT+String.join(" ", tokenStrings));
			}
			executeTokenStrings(tokenStrings, outputResultToConsole);
		}
	}

	private void renderCommandResult(CommandResult commandResult) {
		if(commandResult == null) { return; }
		commandResult.renderToConsole(this);
	}

	private Class<? extends Command> executeTokenStrings(List<String> tokenStrings, boolean outputResultToConsole) {
		return executeTokenStrings(tokenStrings, false, outputResultToConsole);
	}
	
	private Class<? extends Command> executeTokenStrings(List<String> tokenStrings, 
			boolean requireModeWrappable, 
			boolean outputResultToConsole) {
		CommandFactory commandFactory = commandContext.peekCommandMode().getCommandFactory();
		Class<? extends Command> commandClass = commandFactory.identifyCommandClass(commandContext, tokenStrings);
		if(commandClass == null) {
			throw new CommandException(CommandException.Code.UNKNOWN_COMMAND, String.join(" ", tokenStrings), commandContext.getModePath());
		}
		boolean enterModeCmd = commandClass.getAnnotation(EnterModeCommandClass.class) != null;

		String[] commandWords = CommandUsage.cmdWordsForCmdClass(commandClass);
		LinkedList<String> argStrings = new LinkedList<String>(tokenStrings.subList(commandWords.length, tokenStrings.size()));
		LinkedList<String> innerCmdWords = null;
		if(enterModeCmd) {
			@SuppressWarnings("unchecked")
			EnterModeCommandDescriptor entModeCmdDescriptor = 
			EnterModeCommandDescriptor.getDescriptorForClass(commandClass);
			int numEnterModeArgs = entModeCmdDescriptor.enterModeArgNames().length;
			if(numEnterModeArgs < argStrings.size()) {
				innerCmdWords = new LinkedList<String>(argStrings.subList(numEnterModeArgs, argStrings.size()));
				argStrings = new LinkedList<String>(argStrings.subList(0, numEnterModeArgs));
			}
		}
		commandContext.checkCommmandIsExecutable(commandClass);
		if(CommandUsage.hasMetaTagForCmdClass(commandClass, CmdMeta.inputIsComplex)) {
			String cmdWords = String.join(" ", CommandUsage.cmdWordsForCmdClass(commandClass));
			throw new ConsoleException(Code.COMMAND_HAS_COMPLEX_INPUT, cmdWords);
		}
		Command command = buildCommand(commandContext, commandClass, argStrings, this);
		// combine enter-mode command with inner commands.
		if(enterModeCmd && innerCmdWords != null && !innerCmdWords.isEmpty()) {
			commandContext.setRequireModeWrappable(true);
			command.execute(commandContext);
			Class<? extends Command> innerCmdClass = null;
			try {
				innerCmdClass = executeTokenStrings(innerCmdWords, true, outputResultToConsole);
				return innerCmdClass;
			} finally {
				commandContext.setRequireModeWrappable(false);
				// case where innermost command is an enter mode command, stay in that mode.
				// otherwise pop the mode.
				if(innerCmdClass == null || innerCmdClass.getAnnotation(EnterModeCommandClass.class) == null) {
					commandContext.popCommandMode();
				}
			}
		} else {
			CommandResult commandResult = command.execute(commandContext);
			if(outputResultToConsole) {
				renderCommandResult(commandResult);
			}
			return commandClass;
		}
	}

	public static Command buildCommand(
			ConsoleCommandContext commandContext,
			Class<? extends Command> commandClass,
			List<String> argStrings) {
		return buildCommand(commandContext, commandClass, argStrings, null);
	}
	
	private static Command buildCommand(
			ConsoleCommandContext commandContext,
			Class<? extends Command> commandClass,
			List<String> argStrings,
			Console console) {
		Map<String, Object> docoptMap;
		String docoptUsageSingleWord = CommandUsage.docoptStringForCmdClass(commandClass, true);
		docoptMap = runDocopt(commandClass, docoptUsageSingleWord, argStrings);
		CommandBuilder<?, ?> commandBuilder = buildCommandElement(commandContext, commandClass, docoptMap);
		boolean enterModeCmd = commandClass.getAnnotation(EnterModeCommandClass.class) != null;
		Command command;
		try {
			command = commandBuilder.build();
		} catch(PluginConfigException pce) {
			if(pce.getCode() == PluginConfigException.Code.PROPERTY_FORMAT_ERROR &&
					pce.getErrorArgs().length >= 3) {
				throw new CommandException(CommandException.Code.ARGUMENT_FORMAT_ERROR,
						pce.getErrorArgs()[0].toString(),
						pce.getErrorArgs()[1], pce.getErrorArgs()[2]);
			} else {
				throw pce;
			}
		}
		if(!enterModeCmd && console != null) {
			if(commandContext.getOptionValue(ConsoleOption.ECHO_CMD_XML).equals("true")) {
				console.output(new String(GlueXmlUtils.prettyPrint(command.getCmdElem().getOwnerDocument())));
			}
			if(commandContext.getOptionValue(ConsoleOption.ECHO_CMD_JSON).equals("true")) {
				console.output(JsonUtils.prettyPrint(JsonUtils.documentToJSonObjectBuilder(command.getCmdElem().getOwnerDocument()).build()));
			}
		}
		return command;
	}

	private static Map<String, Object> runDocopt(Class<? extends Command> commandClass,
			String docoptUsageSingleWord, List<String> argStrings) {
		Map<String, Object> docoptMap;
		try {
			docoptMap = new Docopt(docoptUsageSingleWord).withHelp(false).withExit(false).parse(argStrings);
		} catch(DocoptExitException dee) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, CommandUsage.docoptStringForCmdClass(commandClass, false).trim());
		}
		return docoptMap;
	}

	@SuppressWarnings("unchecked")
	private static CommandBuilder<?, ?> buildCommandElement(CommandContext cmdContext, Class<? extends Command> cmdClass, Map<String, Object> docoptMap) {
		CommandBuilder cmdBuilder = cmdContext.cmdBuilder(cmdClass);
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
				@SuppressWarnings("rawtypes")
				ArrayBuilder arrayBuilder = cmdBuilder.setArray(tagName);
				((Collection <?>) value).forEach(item -> {
					arrayBuilder.add(item.toString());
				});
			} else {
				cmdBuilder.set(tagName, value.toString());
			}
		});
		return cmdBuilder;
	}

	public static void main(String[] args) {
		Docopt docopt = null;
		try(InputStream docoptStream = Console.class.getResourceAsStream("/Console.docopt")) {
			docopt = new Docopt(docoptStream);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} 
		Map<String, Object> docoptResult = docopt.parse(args);
		Console console = new Console();
		setupConsoleOptions(docoptResult, console);
		console.init();
		if(!console.version) {
			GluetoolsEngine.getInstance().dbWarning();
			Object fileString = docoptResult.get("--batch-file");
			if(fileString != null) {
				console.runBatchFile(fileString.toString());
			}
			if(!console.nonInteractive) {
				console.interactiveSession();
			}
		}
		console.shutdown();
	}

	private static void setupConsoleOptions(Map<String, Object> docoptResult, Console console) {
		Object configFileOption = docoptResult.get("--config-file");
		if(configFileOption != null) {
			console.configFilePath = configFileOption.toString();
		}
		Object migrateSchemaOption = docoptResult.get("--migrate-schema");
		if(migrateSchemaOption != null) {
			console.migrateSchema = Boolean.parseBoolean(migrateSchemaOption.toString());
		}
		Object nonInteractiveOption = docoptResult.get("--non-interactive");
		if(nonInteractiveOption != null) {
			console.nonInteractive = Boolean.parseBoolean(nonInteractiveOption.toString());
		}
		Object versionOption = docoptResult.get("--version");
		if(versionOption != null) {
			console.version = Boolean.parseBoolean(versionOption.toString());
		}
	}

	private void init() {
		try {
			this.reader = new ConsoleReader();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		this.out = new PrintWriter(reader.getOutput());
		GluetoolsEngine gluetoolsEngine = GluetoolsEngine.initInstance(configFilePath, this.migrateSchema);
		this.commandContext = new ConsoleCommandContext(gluetoolsEngine, this);
		commandContext.setCommandContextListener(this);
		commandContext.pushCommandMode(new RootCommandMode(gluetoolsEngine.getRootServerRuntime()));
		reader.addCompleter(new ConsoleCompleter(commandContext));
		output(versionLine());
	}

	private void interactiveSession() {
		while(!isFinished()) {
			handleInteractiveLine();
		}
	}

	private void shutdown() {
		while(!(commandContext.peekCommandMode() instanceof RootCommandMode)) {
			commandContext.popCommandMode();
		}
		GluetoolsEngine.shutdown();
	}

	private void runBatchFile(String batchFilePath) {
		String batchContent = null;
		try {
			batchContent = new String(commandContext.loadBytes(batchFilePath));
		} catch(GlueException glueException) {
			handleGlueException(glueException);
			System.exit(1);
		}
		try {
			runBatchCommands(batchFilePath, batchContent);
		} catch(GlueException ge) {
			handleGlueException(ge);
			System.exit(1);
		}
	}

	public void runBatchCommands(String batchFilePath, String batchContent) {
		String[] batchLines = batchContent.split("\n");
		try {
			BatchContext batchContext = new BatchContext(batchFilePath, 1);
			batchContext.batchLineNumber = 1;
			batchContextStack.push(batchContext);
			for(String batchLine: batchLines) {
				try {
					handleLine(batchLine, true, true);
				} catch(GlueException ge) {
					if(ge.getUserData(GLUE_CONSOLE_BATCH_CONTEXT_STACK) == null) {
						ge.putUserData(GLUE_CONSOLE_BATCH_CONTEXT_STACK, new LinkedList<BatchContext>(batchContextStack));
					}
					throw ge;
				}
				batchContext.batchLineNumber++;
			}
		} finally {
			batchContextStack.pop();
		}
	}


	private void handleGlueException(GlueException glueException) {
		outputError(glueException);
	}
	
	@SuppressWarnings("unchecked")
	private void outputError(Throwable exception) {
		if(exception instanceof GlueException) {
			GlueException glueException = (GlueException) exception;
			GlueErrorCode errorCode = glueException.getCode();
			Object[] errorArgs = glueException.getErrorArgs();
			LinkedList<BatchContext> batchContextStack = 
					(LinkedList<BatchContext>) glueException.getUserData(GLUE_CONSOLE_BATCH_CONTEXT_STACK);
			if(batchContextStack != null && !batchContextStack.isEmpty()) {
				boolean first = true;
				for(BatchContext batchContext: batchContextStack) {
					if(first) {
						output("Exception at line "+batchContext.batchLineNumber+" of "+batchContext.batchFilePath);
						first = false;
					} else {
						output("Invoked from line "+batchContext.batchLineNumber+" of "+batchContext.batchFilePath);
					}
				}
			}
			if(glueException instanceof ConsoleException && errorCode == Code.SYNTAX_ERROR && 
					errorArgs.length >= 1 && errorArgs[0] instanceof Integer) {
				int position = (Integer) errorArgs[0];
				output(StringUtils.repeat(" ", position+reader.getPrompt().length())+"^");
				output("Error: Bad syntax");
			} else {
				output("Error: "+glueException.getLocalizedMessage());
			}
		}
		boolean verboseError = commandContext.getOptionValue(ConsoleOption.VERBOSE_ERROR).equals("true");
		if(verboseError) {
			outputStackTrace(exception);
		}
		Throwable cause = exception.getCause();
		while(cause != null && cause != exception) {
			String message = cause.getLocalizedMessage();
			if(message == null) {
				message = cause.getClass().getSimpleName();
			}
			output("Cause: "+message);
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

	@Override
	public void output(String message) {
		out.println(message);
		out.flush();
	}

	private void updatePrompt() {
		reader.setPrompt("Mode path: "+commandContext.getModePath()+"\n"+GLUE_PROMPT);
	}

	@Override
	public void commandModeChanged() {
		updatePrompt();
	}

	@Override
	public String getConsoleOutputFormat() {
		return commandContext.getOptionValue(ConsoleOption.CMD_RESULT_FORMAT);
	}


	private String versionLine() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("GLUEtools version");
		buffer.append(" ").append(GluetoolsEngine.getInstance().getGluecoreProperties().getProperty("version", "unknown"));
		return buffer.toString();
	}
	
	private class BatchContext {
		
		public BatchContext(String batchFilePath, Integer batchLineNumber) {
			super();
			this.batchFilePath = batchFilePath;
			this.batchLineNumber = batchLineNumber;
		}
		String batchFilePath;
		Integer batchLineNumber;
	}
	
}
