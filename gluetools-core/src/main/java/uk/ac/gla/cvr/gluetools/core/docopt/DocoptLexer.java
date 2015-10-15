package uk.ac.gla.cvr.gluetools.core.docopt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DocoptLexer {

		public static enum TokenType {
			// Token types cannot have underscores
			OPTION("-[A-Za-z0-9]"),
			VARIABLE("<[A-Za-z0-9]+>"),
			LITERAL("[A-Za-z0-9]+"),
			ELLIPSIS("\\.\\.\\."),
			SQLEFT("\\["),
			SQRIGHT("\\]"),
			PIPE("\\|"),
			BRLEFT("\\("),
			BRRIGHT("\\)"),
			WHITESPACE("[ \t\f\r\n]+");

			public final String pattern;

			private TokenType(String pattern) {
				this.pattern = pattern;
			}
			
		}

		public static class Token {
			private TokenType type;
			private String data;

			public Token(TokenType type, String data) {
				this.type = type;
				this.data = data;
			}

			@Override
			public String toString() {
				return String.format("(%s:%s)", type.name(), getData());
			}

			public TokenType getType() {
				return type;
			}

			public String getData() {
				return data;
			}
			
		}

		public static List<Token> meaningfulTokens(List<Token> tokens) {
			List<Token> meaningfulTokens = tokens.stream().
					filter(t -> t.getType() != TokenType.WHITESPACE).
					collect(Collectors.toList());
			return meaningfulTokens;
		}

		
		public static ArrayList<Token> lex(String input) {
			// The tokens to return
			ArrayList<Token> tokens = new ArrayList<Token>();

			// Lexer logic begins here
			StringBuffer tokenPatternsBuffer = new StringBuffer();
			for (TokenType tokenType : TokenType.values())
				tokenPatternsBuffer.append(String.format("|(?<%s>%s)", tokenType.name(), tokenType.pattern));
			Pattern tokenPatterns = Pattern.compile(new String(tokenPatternsBuffer.substring(1)));

			// Begin matching tokens
			Matcher matcher = tokenPatterns.matcher(input);
			int lastEnd = 0;
			while(matcher.find()) {
				if(matcher.start() != lastEnd) {
					break;
				}
				lastEnd = matcher.end();
				for(TokenType tokenType: TokenType.values()) {
					String namedGroupResult = matcher.group(tokenType.name());
					if(namedGroupResult != null) {
						tokens.add(new Token(tokenType, namedGroupResult));
						break;
					}
				}
			}
			if(lastEnd < input.length()) {
				throw new RuntimeException("Docopt option parse error");
			}
			return tokens;		
		}
	
}
