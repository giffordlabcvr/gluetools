package uk.ac.gla.cvr.gluetools.core.collation.populating.regex;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorException;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/**
 * Plugin which runs a regular expression (the match pattern) on an input String.
 * If there is a match, the extractor uses its output pattern to create a string which may 
 * contain groups from the match.
 * By default returns the whole match.
 */
@PluginClass(elemName="regexExtractorFormatter")
public class RegexExtractorFormatter implements Plugin {

	
	private List<Pattern> matchPatterns = new ArrayList<Pattern>();
	private Template outputTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		matchPatterns = PluginUtils.configureStringsProperty(configElem, "matchPattern", 0, null)
				.stream()
				.map(rgxString -> PluginUtils.parseRegexPattern("matchPattern", rgxString))
				.collect(Collectors.toList());
		outputTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, "outputTemplate", false);
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
				} else {
					continue;
				}
			}
			if(workingMatcher == null) {
				return null;
			}
			if(outputTemplate == null) {
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
			if(outputTemplate == null) {
				return input;
			}
			variableResolver = new TemplateHashModel() {
				@Override
				public TemplateModel get(String key) {
					if(key.equals("0")) { return new SimpleScalar(input); } else { return null; }
				}
				@Override
				public boolean isEmpty() { return false; }
			};
		}
		StringWriter result = new StringWriter();
		try {
			outputTemplate.process(variableResolver, result);
		} catch (TemplateException e) {
			throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result.toString();
	}

	
	public static String extractAndConvert(String input, RegexExtractorFormatter mainExtractor, 
			List<RegexExtractorFormatter> valueConverters) {
		if(mainExtractor != null) {
			String mainExtractorResult = mainExtractor.matchAndConvert(input);
			if(mainExtractorResult == null) {
				return null;
			} else {
				input = mainExtractorResult;
			}
		}
		if(valueConverters != null) {
			for(RegexExtractorFormatter valueConverter: valueConverters) {
				String valueConverterResult = valueConverter.matchAndConvert(input);
				if(valueConverterResult != null) {
					return valueConverterResult;
				}
			}
		}
		return input;
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
	}
	
	
}
