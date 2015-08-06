package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.feature.FeatureMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"feature"},
	docoptUsages={"<featureName>"},
	description="Enter command mode for a feature")
@EnterModeCommandClass(
		commandModeClass = FeatureMode.class)
public class FeatureCommand extends ReferenceSequenceModeCommand<OkResult>  {

	public static final String FEATURE_NAME = "featureName";
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Feature feature = GlueDataObject.lookup(objContext, Feature.class, Feature.pkMap(getRefSeqName(), featureName));
		cmdContext.pushCommandMode(new FeatureMode(cmdContext, this, feature.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends FeatureNameCompleter {}	

}
