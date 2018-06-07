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
package uk.ac.gla.cvr.gluetools.core.collation.populating.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/**
 * Plugin which runs a regular expression (the match pattern) on an input String.
 * If there is a match, the extractor uses its output pattern to create a string which may 
 * contain groups from the match.
 * By default returns the whole match.
 */
@PluginClass(elemName="regexExtractorFormatter")
public class RegexExtractorFormatter implements MatcherConverter {

	
	private List<Pattern> matchPatterns = new ArrayList<Pattern>();
	private Template outputTemplate;
	private String outputString;
	// alternative to outputTemplate / outputString which can be used to thrown an error.
	private Template errorTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		matchPatterns = PluginUtils.configureStringsProperty(configElem, "matchPattern", 0, null)
				.stream()
				.map(rgxString -> PluginUtils.parseRegexPattern("matchPattern", rgxString))
				.collect(Collectors.toList());
		outputTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, "outputTemplate", false);
		outputString = PluginUtils.configureStringProperty(configElem, "outputString", false);
		errorTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, "errorTemplate", false);
		int numDefined = (outputTemplate != null ? 1 : 0) + (outputString != null ? 1 : 0) + (errorTemplate != null ? 1 : 0);
		if(numDefined > 1) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "No more than one of outputTemplate, outputString or errorTemplate may be defined");
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	public String matchAndConvert(String input) {
		TemplateHashModel variableResolver;
		if(!matchPatterns.isEmpty()) {
			Matcher workingMatcher = null;
			for(Pattern matchPattern: matchPatterns) {
				final Matcher candidateMatcher = matchPattern.matcher(input);
				if(candidateMatcher.find()) {
					workingMatcher = candidateMatcher;
					break;
				}
			}
			if(workingMatcher == null) {
				return null;
			}
			if(outputTemplate == null && errorTemplate == null) {
				if(outputString != null) {
					return outputString;
				}
				return workingMatcher.group(0);
			}
			final Matcher workingMatcherF = workingMatcher;
			variableResolver = new TemplateHashModel() {
				@Override
				public TemplateModel get(String key) {
					String matcherGroup;
					if(key.matches("g\\d+")) {
						matcherGroup = workingMatcherF.group(Integer.parseInt(key.substring(1)));
					} else {
						matcherGroup = workingMatcherF.group(key);
					}
					if(matcherGroup == null) {
						return null;
					} else {
						return new SimpleScalar(matcherGroup);
					}
				}
				@Override
				public boolean isEmpty() { return false; }
			};
		} else {
			if(outputTemplate == null && errorTemplate == null) {
				if(outputString != null) {
					return outputString;
				}
				return input;
			}
			variableResolver = new TemplateHashModel() {
				@Override
				public TemplateModel get(String key) {
					if(key.equals("g0")) { return new SimpleScalar(input); } else { return null; }
				}
				@Override
				public boolean isEmpty() { return false; }
			};
		}
		if(outputTemplate != null) {
			return FreemarkerUtils.processTemplate(outputTemplate, variableResolver);
		} else {
			// errorTemplate != null;
			String errorMsg = "Error condition in regexExtractorFormatter";
			try {
				errorMsg = FreemarkerUtils.processTemplate(errorTemplate, variableResolver);
			} catch(Exception e) {
				GlueLogger.log(Level.WARNING, "Unable to generate error message for regexExtractorFormatter error template");
			}
			throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, errorMsg);
		}
	}

	
	public List<Pattern> getMatchPatterns() {
		return matchPatterns;
	}

	public void setMatchPatterns(List<Pattern> matchPatterns) {
		this.matchPatterns = matchPatterns;
	}

	public Template getOutputTemplate() {
		return outputTemplate;
	}

	public void setOutputTemplate(Template outputTemplate) {
		this.outputTemplate = outputTemplate;
		this.outputString = null;
	}

	public String getOutputString() {
		return outputString;
	}

	public void setOutputString(String outputString) {
		this.outputString = outputString;
		this.outputTemplate = null;
	}
	
}
