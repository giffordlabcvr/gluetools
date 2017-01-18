package uk.ac.gla.cvr.gluetools.core.console;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import jline.console.completer.Completer;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandDescriptor;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.Token;
import uk.ac.gla.cvr.gluetools.core.console.Lexer.TokenType;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;

@SuppressWarnings("rawtypes")
public class ConsoleCompleter implements Completer {

	private ConsoleCommandContext cmdContext;
	
	public ConsoleCompleter(ConsoleCommandContext cmdContext) {
		super();
		this.cmdContext = cmdContext;
	}

	@Override
	public int complete(String buffer, int cursor, List<CharSequence> candidates) {
		List<Token> tokens;
		int cursorOffset = 0;
		boolean backslashAtEnd = false;
		boolean quoteWasAdded = false;
		boolean addSpaceForCompleted = true;
		try {
			tokens = Lexer.lex(buffer);
		} catch(ConsoleException ce) {
			if(ce.getCode().equals(ConsoleException.Code.SYNTAX_ERROR)) {
				cursorOffset = 1;
				try {
					tokens = Lexer.lex(buffer+"\"");
					quoteWasAdded = true;
				} catch(ConsoleException ce2) {
					try {
						tokens = Lexer.lex(buffer+"'");
						quoteWasAdded = true;
					} catch(ConsoleException ce3) {
						if(buffer.endsWith("\\")) {
							cursorOffset = 0;
							backslashAtEnd = true;
							try {
								tokens = Lexer.lex(buffer.substring(0, buffer.length()-1)+"\"");
								quoteWasAdded = true;
							} catch(ConsoleException ce4) {
								try {
									tokens = Lexer.lex(buffer.substring(0, buffer.length()-1)+"'");
									quoteWasAdded = true;
								} catch(ConsoleException ce5) {
									throw ce;
								}
							}
						} else {
							throw ce;
						}
					}
				}
			} else {
				throw ce;
			}
		}
		if(tokens.stream().filter(t -> t.getType() == TokenType.SINGLELINECOMMENT).findFirst().isPresent()) {
			return -1;
		}
		List<Token> meaningfulTokens = Lexer.meaningfulTokens(tokens);
		int suggestionPos;
		List<Token> lookupBasisTokens;
		String prefix;
		TokenType finalTokenType = TokenType.WORD;
		if(meaningfulTokens.isEmpty()) {
			suggestionPos = cursor;
			lookupBasisTokens = new LinkedList<Token>();
			prefix = "";
		} else {
			// cursor is not after the last meaningful token
			Token lastMeaningfulToken = meaningfulTokens.get(meaningfulTokens.size()-1);
			finalTokenType = lastMeaningfulToken.getType();
			int endOfLMT = lastMeaningfulToken.getPosition() + lastMeaningfulToken.getData().length();
			if(cursor + cursorOffset < endOfLMT) {
				lookupBasisTokens = new LinkedList<Token>();
				Token incompleteToken = null;
				Token lastCompleteToken = null;
				int effectiveCursor = cursor+cursorOffset;
				for(Token token: meaningfulTokens) {
					if(effectiveCursor < token.getPosition()) {
						break;
					}
					if(effectiveCursor > token.getPosition() && 
							effectiveCursor < (token.getPosition()+token.getData().length())) {
						incompleteToken = token;
					} else {
						if(effectiveCursor >= (token.getPosition()+token.getData().length())) {
							lookupBasisTokens.add(token);
							lastCompleteToken = token;
						}
					}
				}
				if(incompleteToken == null && lastCompleteToken != null) {
					int endOfLCT = lastCompleteToken.getPosition() + lastCompleteToken.getData().length();
					if(effectiveCursor == endOfLCT) {
						// cursor is just after the end of the last complete token
						suggestionPos = lastCompleteToken.getPosition();
						lookupBasisTokens.remove(lastCompleteToken);
						prefix = lastCompleteToken.getType().render(lastCompleteToken.getData());
						addSpaceForCompleted = false;
					} else {
						// spaces between last complete token and cursor.
						suggestionPos = cursor;
						prefix = "";
					}
				} else {
					return -1;
				}
			
			} else {
				// cursor is just after the end of the last meaningful token
				if(cursor + cursorOffset == endOfLMT) {
					suggestionPos = lastMeaningfulToken.getPosition();
					lookupBasisTokens = meaningfulTokens.subList(0, meaningfulTokens.size()-1);
					prefix = lastMeaningfulToken.getType().render(lastMeaningfulToken.getData());
				} else {
					// spaces between last meaningful token and cursor.
					suggestionPos = cursor;
					lookupBasisTokens = meaningfulTokens;
					prefix = "";
				}
			}
		}
		List<String> lookupBasis = lookupBasisTokens.stream().map(Token::render).collect(Collectors.toList());
		
		return completeAux(candidates, suggestionPos, prefix, lookupBasis, false, finalTokenType, backslashAtEnd, quoteWasAdded, addSpaceForCompleted);
	}

	private int completeAux(List<CharSequence> candidates, int suggestionPos,
			String prefix, List<String> lookupBasis, boolean requireModeWrappable, TokenType finalTokenType, 
			boolean backslashAtEnd, boolean quoteWasAdded, boolean addSpaceForCompleted) {
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
			boolean enterModeSucceded = false;
			try {
				enterModeCommand.execute(cmdContext);
				enterModeSucceded = true;
				return completeAux(candidates, suggestionPos, prefix, innerCmdWords, true, finalTokenType, backslashAtEnd, quoteWasAdded, addSpaceForCompleted);
			} catch(DataModelException dme) {
				if(dme.getCode() == DataModelException.Code.OBJECT_NOT_FOUND) {
					// enter mode command failed because we were unable to look up the object.
					return -1;
				} else {
					throw dme;
				}
			} finally {
				if(enterModeSucceded) {
					cmdContext.popCommandMode();
				}
			}
		} else {
			List<CompletionSuggestion> completionSuggestions = commandFactory.getCommandWordSuggestions(cmdContext, lookupBasis, prefix, true, requireModeWrappable, true);

			List<CompletionSuggestion> suggestions = completionSuggestions.
					stream().filter(s -> s.getSuggestedWord().startsWith(prefix)).collect(Collectors.toList());
			if(suggestions.isEmpty()) {
				return -1;
			} else {
				List<String> unfilteredCandidates = suggestions.stream().map(s -> {
					String suggestedWord = s.getSuggestedWord();
					boolean completed = s.isCompleted();
					suggestedWord = escapeSuggestion(finalTokenType, suggestedWord, quoteWasAdded, completed);
					if(completed && addSpaceForCompleted) {
						return suggestedWord+" ";
					} else {
						return suggestedWord;
					}}).collect(Collectors.toList());

				String escapedPrefix = escapeSuggestion(finalTokenType, prefix, quoteWasAdded, false) + (backslashAtEnd ? "\\" : "");
				List<String> filteredCandidates = unfilteredCandidates.stream().filter(s -> s.startsWith(escapedPrefix)).collect(Collectors.toList());
				candidates.addAll(filteredCandidates);
				return suggestionPos;
			} 
		}
	}

	private String escapeSuggestion(TokenType finalTokenType,
			String suggestedWord, boolean quoteWasAdded, boolean completed) {
		if(quoteWasAdded && finalTokenType == TokenType.DOUBLEQUOTED) {
			suggestedWord = Lexer.toDoubleQuoted(suggestedWord);
			if(!completed) {
				suggestedWord = suggestedWord.substring(0, suggestedWord.length()-1);
			}
		} else if(quoteWasAdded && finalTokenType == TokenType.SINGLEQUOTED) {
			suggestedWord = Lexer.toSingleQuoted(suggestedWord);
			if(!completed) {
				suggestedWord = suggestedWord.substring(0, suggestedWord.length()-1);
			}
		} else {
			suggestedWord = Lexer.escaped(suggestedWord);
		}
		return suggestedWord;
	}


}
