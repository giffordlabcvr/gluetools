package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","feature"}, 
	docoptUsages={"<featureName> [<description>]"},
	description="Create a new alignment feature", 
	furtherHelp="A feature is a (possibly non-contiguous) region of the alignment's reference sequence which is of particular interest.") 
public class CreateFeatureCommand extends AlignmentModeCommand {

	public static final String FEATURE_NAME = "featureName";
	public static final String DESCRIPTION = "description";
	
	private String featureName;
	private Optional<String> description;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment alignment = GlueDataObject.lookup(cmdContext.getObjectContext(), Alignment.class, 
				Alignment.pkMap(getAlignmentName()));

		Feature feature = GlueDataObject.create(objContext, Feature.class, Feature.pkMap(getAlignmentName(), featureName), false);
		feature.setAlignment(alignment);
		description.ifPresent(d -> {feature.setDescription(d);});
		return new CreateResult(Feature.class, 1);
	}

}
