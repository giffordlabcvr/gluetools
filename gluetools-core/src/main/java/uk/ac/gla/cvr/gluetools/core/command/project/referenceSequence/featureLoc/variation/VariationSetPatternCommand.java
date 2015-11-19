package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"set","pattern"}, 
		docoptUsages={"<regex>"},
		metaTags={CmdMeta.updatesDatabase},
		description="Set the variation's regular expression pattern") 
public class VariationSetPatternCommand extends VariationModeCommand<OkResult> {

	public static final String REGEX = "regex";
	
	private Pattern regex;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		regex = PluginUtils.configureRegexPatternProperty(configElem, REGEX, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		variation.setRegex(regex.pattern());
		cmdContext.commit();
		return new UpdateResult(Variation.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}

	
}
