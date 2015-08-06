package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "feature"}, 
	docoptUsages={"<featureName>"},
	description="Delete a reference sequence feature") 
public class DeleteFeatureCommand extends ReferenceSequenceModeCommand<DeleteResult> {

	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		DeleteResult result = GlueDataObject.delete(objContext, Feature.class, Feature.pkMap(getRefSeqName(), featureName));
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends FeatureNameCompleter {}

}
