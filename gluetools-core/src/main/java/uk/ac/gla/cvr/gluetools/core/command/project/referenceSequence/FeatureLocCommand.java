package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"feature-location"},
	docoptUsages={"<featureName>"},
	description="Enter command mode for a feature location")
@EnterModeCommandClass(
		commandModeClass = FeatureLocMode.class)
public class FeatureLocCommand extends ReferenceSequenceModeCommand<OkResult>  {

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
		FeatureLocation featureLoc = GlueDataObject.lookup(objContext, FeatureLocation.class, FeatureLocation.pkMap(getRefSeqName(), featureName));
		cmdContext.pushCommandMode(new FeatureLocMode(getRefSeqMode(cmdContext).getProject(), this, 
				featureLoc.getReferenceSequence().getName(),
				featureLoc.getFeature().getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends FeatureNameCompleter {}	

}
