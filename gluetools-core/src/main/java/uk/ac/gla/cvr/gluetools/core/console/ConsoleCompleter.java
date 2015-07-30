package uk.ac.gla.cvr.gluetools.core.console;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import jline.console.completer.Completer;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandDescriptor;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.Token;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.TokenType;

public class ConsoleCompleter implements Completer {

	private ConsoleCommandContext cmdContext;
	
	public ConsoleCompleter(ConsoleCommandContext cmdContext) {
		super();
		this.cmdContext = cmdContext;
	}

	@Override
	public int complete(String buffer, int cursor, List<CharSequence> candidates) {
		List<Token> tokens = Lexer.lex(buffer);
		if(tokens.stream().filter(t -> t.getType() == TokenType.SINGLELINECOMMENT).findFirst().isPresent()) {
			return -1;
		}
		List<Token> meaningfulTokens = Lexer.meaningfulTokens(tokens);
		int suggestionPos;
		List<Token> lookupBasisTokens;
		String prefix;
		if(meaningfulTokens.isEmpty()) {
			suggestionPos = cursor;
			lookupBasisTokens = new LinkedList<Token>();
			prefix = "";
		} else {
			// if the cursor is not after the last meaningful token, return no matches.
			Token lastMeaningfulToken = meaningfulTokens.get(meaningfulTokens.size()-1);
			int endOfLMT = lastMeaningfulToken.getPosition() + lastMeaningfulToken.getData().length();
			if(cursor < endOfLMT) {
				return -1;
			}
			// cursor is just after the end of the last meaningful token
			if(cursor == endOfLMT) {
				suggestionPos = lastMeaningfulToken.getPosition();
				lookupBasisTokens = meaningfulTokens.subList(0, meaningfulTokens.size()-1);
				prefix = lastMeaningfulToken.getData();
			} else {
				// spaces between last meaningful token and cursor.
				suggestionPos = cursor;
				lookupBasisTokens = meaningfulTokens;
				prefix = "";
			}
		}
		List<String> lookupBasis = lookupBasisTokens.stream().map(Token::render).collect(Collectors.toList());
		
		return completeAux(candidates, suggestionPos, prefix, lookupBasis, false);
	}

	private int completeAux(List<CharSequence> candidates, int suggestionPos,
			String prefix, List<String> lookupBasis, boolean requireModeWrappable) {
		// System.out.println("completeAux: position "+suggestionPos+", prefix "+prefix+", lookupBasis "+lookupBasis);
		CommandMode<?> cmdMode = cmdContext.peekCommandMode();
		CommandFactory commandFactory = cmdMode.getCommandFactory();
		Class<? extends Command> cmdClass = commandFactory.identifyCommandClass(cmdContext, lookupBasis);
		boolean enterModeCmd = cmdClass != null && cmdClass.getAnnotation(EnterModeCommandClass.class) != null;
		List<String> innerCmdWords = null;
		List<String> enterModeArgStrings = null;
		if(enterModeCmd) {
			@SuppressWarnings("unchecked")
			EnterModeCommandDescriptor entModeCmdDescriptor = 
			EnterModeCommandDescriptor.getDescriptorForClass(cmdClass);
			int numCmdWords = CommandUsage.cmdWordsForCmdClass(cmdClass).length;
			int numEnterModeArgs = entModeCmdDescriptor.enterModeArgNames().length;
			enterModeArgStrings = lookupBasis.subList(numCmdWords, lookupBasis.size());
			if(numEnterModeArgs <= enterModeArgStrings.size()) {
				innerCmdWords = new LinkedList<String>(enterModeArgStrings.subList(numEnterModeArgs, enterModeArgStrings.size()));
				enterModeArgStrings = new LinkedList<String>(enterModeArgStrings.subList(0, numEnterModeArgs));
			}
			//System.out.println("numEnterModeArgs: "+numEnterModeArgs+", innerCmdWords: "+innerCmdWords+", lookupBasis: "+lookupBasis);
		}

		if(enterModeCmd && innerCmdWords != null) {
			Command enterModeCommand = Console.buildCommand(cmdContext, cmdClass, enterModeArgStrings);
			try {
				enterModeCommand.execute(cmdContext);
				return completeAux(candidates, suggestionPos, prefix, innerCmdWords, true);
			} finally {
				cmdContext.popCommandMode();
			}
		} else {
			List<String> suggestions = commandFactory.getCommandWordSuggestions(cmdContext, lookupBasis, true, requireModeWrappable).
					stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
			if(suggestions.isEmpty()) {
				return -1;
			} else {
				candidates.addAll(suggestions.stream().map(s -> s+" ").collect(Collectors.toList()));
				return suggestionPos;
			} 
		}
	}


}
