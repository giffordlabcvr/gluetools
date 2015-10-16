package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"add","feature-location"}, 
	docoptUsages={"<featureName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Define a feature's location on the reference", 
	furtherHelp="Define a (possibly non-contiguous) region of the reference sequence which is where a named genome feature is located.") 
public class AddFeatureLocCommand extends ReferenceSequenceModeCommand<CreateResult> {

	public static final String FEATURE_NAME = "featureName";
	
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		ReferenceSequence referenceSequence = lookupRefSeq(cmdContext);
		Feature feature = GlueDataObject.lookup(cmdContext.getObjectContext(), Feature.class, 
				Feature.pkMap(featureName));
		FeatureLocation featureLoc = GlueDataObject.create(objContext, FeatureLocation.class, FeatureLocation.pkMap(referenceSequence.getName(), feature.getName()), false);
		featureLoc.setReferenceSequence(referenceSequence);
		featureLoc.setFeature(feature);
		cmdContext.commit();
		return new CreateResult(FeatureLocation.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("featureName", Feature.class, Feature.NAME_PROPERTY);
		}
	}

}
