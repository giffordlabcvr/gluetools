package uk.ac.gla.cvr.gluetools.core.collation.populating.regex;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorException;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
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

	
	private Optional<Pattern> matchPattern;
	private Template outputTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		matchPattern = Optional.ofNullable(PluginUtils.configureRegexPatternProperty(configElem, "matchPattern", false));
		String outputPatternXPathExpression = "outputPattern/text()";
		String templateString = PluginUtils.configureString(configElem, outputPatternXPathExpression, false);
		Configuration freemarkerConfiguration = pluginConfigContext.getFreemarkerConfiguration();
		if(templateString != null) {
			try {
				// TODO plugins should have unique IDs we can use here?
				outputTemplate = new Template(UUID.randomUUID().toString(), new StringReader(templateString), freemarkerConfiguration);
			} catch(ParseException pe) {
				throw new PluginConfigException(pe, Code.CONFIG_FORMAT_ERROR, outputPatternXPathExpression, pe.getLocalizedMessage(), templateString);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} 
		}
	}
	
	@SuppressWarnings("rawtypes")
	public String matchAndConvert(String input) {
		TemplateHashModel variableResolver;
		if(matchPattern.isPresent()) {
			final Matcher matcher = matchPattern.get().matcher(input);
			if(!matcher.find()) {
				return null;
			}
			if(outputTemplate == null) {
				return matcher.group(0);
			}
			variableResolver = new TemplateHashModel() {
				@Override
				public TemplateModel get(String key) {
					String matcherGroup;
					if(key.matches("g\\d+")) {
						matcherGroup = matcher.group(Integer.parseInt(key.substring(1)));
					} else {
						matcherGroup = matcher.group(key);
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
	
	
}