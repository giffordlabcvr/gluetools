package uk.ac.gla.cvr.gluetools.core.console;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandDescriptor;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleOptionCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.Token;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilderException;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLoggingFormatter;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

// TODO command lines ending with '\' should be concatenated to allow continuations.
// TODO improve command line table display to adapt columns to data.
// TODO implement paging of commands in interactive mode.
// TODO catch general exceptions and continue.
// TODO history should be stored in the DB.
// TODO in batch mode, exceptions should go to stderr.
@SuppressWarnings("rawtypes")
public class Console implements CommandResultRenderingContext
{
	private static final String GLUE_PROMPT = "GLUE> ";
	private static final String GLUE_CONSOLE_BATCH_CONTEXT_STACK = "glue.console.batchContextStack";
	private PrintWriter out;
	private ConsoleReader reader;
	private ConsoleCommandContext commandContext;
	private LinkedList<BatchContext> batchContextStack = new LinkedList<BatchContext>();
	private String configFilePath = null;
	private Map<ConsoleOption, String> inlineConsoleOptions = null;
	private boolean nonInteractive = true;
	private boolean migrateSchema;
	private boolean version;
	private boolean noEcho = false;
	private boolean noOutput = false;
	private String batchFilePath;
	private List<String> inlineCmdWords;
	
	private Console() {
	}
	
	private boolean isFinished() {
		return commandContext.isFinished();
	}

	private void handleInteractiveLine() {
		String line = null;
		modePathAndOptionLines();
		try {
			line = reader.readLine();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		try {
			handleLine(line, false, true);
		} catch(GlueException ge) {
			commandContext.newObjectContext(); // due to error, the current object context may lack integrity.
			handleGlueException(ge);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleLine(Object line, boolean outputCommandToConsole, boolean outputResultToConsole) {
		List<String> tokenStrings;
		String cmdEcho;
		if(line instanceof String) {
			ArrayList<Token> tokens = null;
			tokens = Lexer.lex((String) line);
			cmdEcho = GLUE_PROMPT+String.join(" ", ((String) line).trim());
			List<Token> meaningfulTokens = Lexer.meaningfulTokens(tokens);
			tokenStrings = meaningfulTokens.stream().map(t -> t.render()).collect(Collectors.toList());
		} else if(line instanceof List) {
			tokenStrings = (List<String>) line;
			cmdEcho = GLUE_PROMPT+String.join(" ", (List<String>) line);  // should really quotify the line.
		} else {
			throw new RuntimeException("Unrecognized line type");
		}
		if(!tokenStrings.isEmpty()) {
			if(outputCommandToConsole) {
				output(cmdEcho);
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
		Console console = new Console();
		List<String> argsList = Arrays.stream(args).collect(Collectors.toList());
		for(int i = 0; i < argsList.size(); i++) {
			if(argsList.get(i).equals("-i") || argsList.get(i).equals("--inline-cmd")) {
				if(i < argsList.size() - 1) {
					console.inlineCmdWords = argsList.subList(i+1, argsList.size());
					argsList = argsList.subList(0, i);
					break;
				}
			}
		}
		Map<String, Object> docoptResult = docopt.parse(argsList.toArray(new String[]{}));
		setupConsoleOptions(docoptResult, console);
		console.init();
		if(console.version) {
			console.output(console.versionLine());
		} else {
			GluetoolsEngine.getInstance().dbWarning();
			if(console.batchFilePath != null) {
				console.runBatchFile(console.batchFilePath, console.noEcho, console.noOutput);
			}
			if(console.inlineCmdWords != null) {
				console.runInlineCommand(console.inlineCmdWords, console.noEcho, console.noOutput);
			}
			if((console.inlineCmdWords == null || console.inlineCmdWords.isEmpty()) && !console.nonInteractive) {
				console.interactiveSession();
			}
		}
		console.shutdown();
	}

	@SuppressWarnings("unchecked")
	private static void setupConsoleOptions(Map<String, Object> docoptResult, Console console) {
		Object configFileOption = docoptResult.get("--config-file");
		if(configFileOption != null) {
			console.configFilePath = configFileOption.toString();
		}
		Object batchFileOption = docoptResult.get("--batch-file");
		if(batchFileOption != null) {
			console.batchFilePath = batchFileOption.toString();
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
		Object consoleOptionOption = docoptResult.get("--console-option");
		if(consoleOptionOption != null) {
			@SuppressWarnings("unchecked")
			List<Object> optionNameValuePairs = (List<Object>) consoleOptionOption;
			console.inlineConsoleOptions = new LinkedHashMap<ConsoleOption, String>();
			for(Object nameValuePairObj : optionNameValuePairs) {
				String nameValuePair = (String) nameValuePairObj;
				int indexOfColon = nameValuePair.indexOf(':');
				if(indexOfColon == -1) {
					throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Inline console option does not contain \":\"");
				}
				String name = nameValuePair.substring(0, indexOfColon);
				ConsoleOption consoleOption = ConsoleOptionCommand.lookupOptionByName(name);
				console.inlineConsoleOptions.put(consoleOption, nameValuePair.substring(indexOfColon+1, nameValuePair.length()));
			}
		}
		Object noEchoOption = docoptResult.get("--no-echo");
		if(noEchoOption != null) {
			console.noEcho = Boolean.parseBoolean(noEchoOption.toString());
		}
		Object noOutputOption = docoptResult.get("--no-output");
		if(noOutputOption != null) {
			console.noOutput = Boolean.parseBoolean(noOutputOption.toString());
		}
	}

	private void init() {
		try {
			this.reader = new ConsoleReader();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		this.out = new PrintWriter(reader.getOutput());
		reader.setPrompt(GLUE_PROMPT);
		reader.setExpandEvents(false);
		GlueLogger.getGlueLogger().setUseParentHandlers(false);
		ConsoleLoggerHandler handler = new ConsoleLoggerHandler(this);
		handler.setFormatter(new GlueLoggingFormatter());
		GlueLogger.getGlueLogger().addHandler(handler);
		GluetoolsEngine gluetoolsEngine = null;
		try {
			gluetoolsEngine = GluetoolsEngine.initInstance(configFilePath, this.migrateSchema);
		} catch(ModelBuilderException mbe) {
			System.err.println(mbe.getLocalizedMessage());
			System.exit(1);
		}
		this.commandContext = new ConsoleCommandContext(gluetoolsEngine, this);
		if(inlineConsoleOptions != null) {
			inlineConsoleOptions.forEach((optionName, optionValue) -> {
				this.commandContext.setOptionValue(optionName, optionValue);
			});
		}
		commandContext.pushCommandMode(new RootCommandMode(gluetoolsEngine.getRootServerRuntime()));
		reader.addCompleter(new ConsoleCompleter(commandContext));
	}

	private void interactiveSession() {
		output(versionLine());
		runGlueRC();
		while(!isFinished()) {
			handleInteractiveLine();
		}
	}

	private void runGlueRC() {
		String userHome = System.getProperty("user.home");
		if(userHome == null) {
			return;
		}
		File userHomeFile = new File(userHome);
		if(!userHomeFile.isDirectory()) {
			return;
		}
		File glueRCFile = new File(userHomeFile, ".gluerc");
		if(glueRCFile.isFile() && glueRCFile.canRead()) {
			GlueLogger.getGlueLogger().finest("Running .gluerc from "+userHome);
			runBatchFile(glueRCFile.getAbsolutePath(), true, true);
		}
	}

	private void shutdown() {
		while(!(commandContext.peekCommandMode() instanceof RootCommandMode)) {
			commandContext.popCommandMode();
		}
		GluetoolsEngine.shutdown();
	}

	private void runBatchFile(String batchFilePath, boolean noEcho, boolean noOutput) {
		String batchContent = null;
		try {
			batchContent = new String(commandContext.loadBytes(batchFilePath));
		} catch(GlueException glueException) {
			handleGlueException(glueException);
			System.exit(1);
		}
		try {
			String[] lines = batchContent.split("\n");
			runBatchCommands(batchFilePath, Arrays.stream(lines).collect(Collectors.toList()), noEcho, noOutput);
		} catch(GlueException ge) {
			handleGlueException(ge);
			System.exit(1);
		}
	}
	
	private void runInlineCommand(List<String> inlineCmdWords, boolean noEcho, boolean noOutput) {
		try {
			runBatchCommands("inline-command", Collections.singletonList(inlineCmdWords), noEcho, noOutput);
		} catch(GlueException ge) {
			handleGlueException(ge);
			System.exit(1);
		}
	}

	public void runBatchCommands(String batchFilePath, List<Object> batchLines, boolean noEcho, boolean noOutput) {
		String initialModePath = commandContext.getModePath();
		boolean requireModeWrappable = commandContext.isRequireModeWrappable();
		try {
			commandContext.setRequireModeWrappable(false);
			BatchContext batchContext = new BatchContext(batchFilePath, 1);
			batchContext.batchLineNumber = 1;
			batchContextStack.push(batchContext);
			for(Object batchLine: batchLines) {
				try {
					handleLine(batchLine, !noEcho, !noOutput);
				} catch(GlueException ge) {
					if(ge.getUserData(GLUE_CONSOLE_BATCH_CONTEXT_STACK) == null) {
						ge.putUserData(GLUE_CONSOLE_BATCH_CONTEXT_STACK, new LinkedList<BatchContext>(batchContextStack));
					}
					throw ge;
				} 
				batchContext.batchLineNumber++;
			}
		} finally {
			commandContext.setRequireModeWrappable(requireModeWrappable);
			batchContextStack.pop();
			while(!commandContext.getModePath().equals(initialModePath)) {
				commandContext.popCommandMode();
			}
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
		output(message, true);
	}

	@Override
	public void output(String message, boolean newLine) {
		if(newLine) {
			out.println(message);
		} else {
			out.print(message);
		}
		out.flush();
	}

	private void modePathAndOptionLines() {
		StringBuffer buf = new StringBuffer();
		buf.append("Mode path: ").append(commandContext.getModePath());
		for(ConsoleOption consoleOption: commandContext.getOptionLines()) {
			buf.append("\n").append("Option ").append(consoleOption.getName()).append(": ");
			String optionValue = commandContext.getOptionValue(consoleOption);
			if(optionValue == null) {
				buf.append("-");
			} else {
				buf.append(optionValue);
			}
		}
		output(buf.toString());
	}

	@Override
	public ConsoleOutputFormat getConsoleOutputFormat() {
		return ConsoleOutputFormat.valueOf(commandContext.getOptionValue(ConsoleOption.CMD_RESULT_FORMAT).toUpperCase());
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

	@Override
	public int getTerminalWidth() {
		return reader.getTerminal().getWidth();
	}

	@Override
	public int getTerminalHeight() {
		return reader.getTerminal().getHeight();
	}
	
	@Override
	public InputStream getInputStream() {
		return reader.getInput();
	}

	@Override
	public boolean interactiveTables() {
		return batchContextStack.isEmpty() // don't use interactive tables in batch mode.
				&& commandContext.getOptionValue(ConsoleOption.INTERACTIVE_TABLES).equals("true");
	}

	@Override
	public Integer floatDecimalPlacePrecision() {
		String optionValue = commandContext.getOptionValue(ConsoleOption.TABLE_RESULT_DOUBLE_PRECISION);
		if(optionValue.equals("full")) {
			return null;
		} else {
			return Integer.parseInt(optionValue);
		}
	}
	
}
