package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "variation"}, 
	docoptUsages={"<variationName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a feature variation") 
public class DeleteVariationCommand extends FeatureLocModeCommand<DeleteResult> {

	private String variationName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		variationName = PluginUtils.configureStringProperty(configElem, "variationName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		DeleteResult result = 
				GlueDataObject.delete(objContext, Variation.class, Variation.pkMap(
						getRefSeqName(), getFeatureName(), variationName), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends VariationNameCompleter {}

}