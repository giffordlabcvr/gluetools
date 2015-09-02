package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","variation"}, 
	docoptUsages={"<variationName> <regex> [<description>]"},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new feature variation", 
	furtherHelp="A variation is a regular expression defining a known motif which may occur at a feature location.") 
public class CreateVariationCommand extends FeatureModeCommand<CreateResult> {

	public static final String VARIATON_NAME = "variationName";
	public static final String DESCRIPTION = "description";
	public static final String REGEX = "regex";
	
	private String variationName;
	private Optional<String> description;
	private Pattern regex;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		variationName = PluginUtils.configureStringProperty(configElem, VARIATON_NAME, true);
		regex = PluginUtils.configureRegexPatternProperty(configElem, REGEX, true);
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Feature feature = lookupFeature(cmdContext);
		Variation variation = GlueDataObject.create(objContext, 
				Variation.class, Variation.pkMap(getFeatureName(), variationName), false);
		description.ifPresent(d -> {variation.setDescription(d);});
		variation.setRegex(regex.pattern());
		variation.setFeature(feature);
		cmdContext.commit();
		return new CreateResult(Variation.class, 1);
	}

}
