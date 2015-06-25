package uk.ac.gla.cvr.gluetools.core.console;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import jline.console.completer.Completer;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.Token;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.TokenType;

public class ConsoleCompleter implements Completer {

	private CommandContext cmdContext;
	
	public ConsoleCompleter(CommandContext cmdContext) {
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
		CommandMode cmdMode = cmdContext.peekCommandMode();
		try {
			cmdContext.setObjectContext(cmdMode.getServerRuntime().getContext());
			CommandFactory commandFactory = cmdMode.getCommandFactory();
			List<String> suggestions = commandFactory.getCommandWordSuggestions(cmdContext, lookupBasis).
					stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
			if(suggestions.isEmpty()) {
				return -1;
			} else {
				candidates.addAll(suggestions.stream().map(s -> s+" ").collect(Collectors.toList()));
				return suggestionPos;
			} 
		} finally {
			cmdContext.setObjectContext(null);
		}
	}

}
