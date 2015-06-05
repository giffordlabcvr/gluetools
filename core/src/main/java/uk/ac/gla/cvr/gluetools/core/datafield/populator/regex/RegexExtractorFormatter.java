package uk.ac.gla.cvr.gluetools.core.datafield.populator.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

/**
 * Plugin which runs a regular expression (the match pattern) on an input String.
 * If there is a match, the extractor uses its output pattern to create a string which may 
 * contain groups from the match.
 * By default the output pattern is "${0}" i.e. the whole match.
 */
public class RegexExtractorFormatter implements Plugin {

	public static final String ELEM_NAME = "regexExtractorFormatter";
	
	private Pattern matchPattern;
	private String outputPattern;
	
	@Override
	public void configure(Element configElem)  {
		String matchPatternXPathExpression = "matchPattern/text()";
		String matchPatternString = PluginUtils.configureString(configElem, matchPatternXPathExpression, false);
		if(matchPatternString != null) {
			try {
				matchPattern = Pattern.compile(matchPatternString);
			} catch(PatternSyntaxException pse) {
				throw new PluginConfigException(pse, Code.CONFIG_FORMAT_ERROR, configElem.getNodeName(), matchPatternXPathExpression, pse.getLocalizedMessage(), matchPatternString);
			}
		} else {
			matchPattern = null;
		}
		outputPattern = PluginUtils.configureString(configElem, "outputPattern/text()", false);
	}
	
	@SuppressWarnings("rawtypes")
	public String matchAndConvert(String input) {
		StrLookup variableResolver;
		if(matchPattern == null) {
			if(outputPattern == null) {
				return input;
			}
			variableResolver = new StrLookup() {
				@Override
				public String lookup(String key) {
					if(key.equals("0")) { return input; } else { return null; }
				}
			};
		} else {
			final Matcher matcher = matchPattern.matcher(input);
			if(!matcher.find()) {
				return null;
			}
			if(outputPattern == null) {
				return matcher.group(0);
			}
			variableResolver = new StrLookup() {
				@Override
				public String lookup(String key) {
					if(key.matches("\\d+")) {
						return matcher.group(Integer.parseInt(key));
					} else {
						return matcher.group(key);
					}
				}
			};
		}
		StrSubstitutor strSubstitutor = new StrSubstitutor();
		strSubstitutor.setEscapeChar('\\');
		strSubstitutor.setVariableResolver(variableResolver);
		StrBuilder strBuilder = new StrBuilder(outputPattern);
		strSubstitutor.replaceIn(strBuilder);
		return strBuilder.toString();
	}

	public String getMatchPattern() {
		return matchPattern.pattern();
	}
}
