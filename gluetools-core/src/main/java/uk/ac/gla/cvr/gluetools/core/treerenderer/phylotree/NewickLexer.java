package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NewickLexer {

	public static enum TokenType {
		// Token types cannot have underscores
		SEMICOLON(";"),
		LEFTPAREN("\\("),
		RIGHTPAREN("\\)"),
		COLON(":"),
		COMMA(","),
		NUMBER("[0-9]+(?:\\.[0-9+])"),
		NAME("[^ \t\f\r\n;:,\\(\\)\\[\\]]+"),
		COMMENT("\\[[^\\[\\]]+\\]"),
		WHITESPACE("[ \t\f\r\n]+");

		public final String pattern;

		private TokenType(String pattern) {
			this.pattern = pattern;
		}
		
		protected String render(String data) {
			return data;
		}
	}

	public static class Token {
		private TokenType type;
		private int position;
		private String data;

		public Token(TokenType type, int position, String data) {
			this.type = type;
			this.position = position;
			this.data = data;
		}

		@Override
		public String toString() {
			return String.format("(%s:%s)", type.name(), render());
		}

		public TokenType getType() {
			return type;
		}

		public int getPosition() {
			return position;
		}
		
		public String getData() {
			return data;
		}

		public String render() {
			return type.render(data);
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
		for (TokenType tokenType : TokenType.values()) {
			tokenPatternsBuffer.append(String.format("|(?<%s>%s)", tokenType.name(), tokenType.pattern));
		}
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
					tokens.add(new Token(tokenType, matcher.start(), namedGroupResult));
					break;
				}
			}
		}
		if(lastEnd < input.length()) {
			throw new PhyloNewickException(PhyloNewickException.Code.SYNTAX_ERROR, lastEnd);
		}
		return tokens;		
	}


	
}