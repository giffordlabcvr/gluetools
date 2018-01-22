/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
