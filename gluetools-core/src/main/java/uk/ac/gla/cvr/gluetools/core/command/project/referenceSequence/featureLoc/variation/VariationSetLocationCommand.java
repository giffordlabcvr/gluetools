package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"set","location"}, 
		docoptUsages={"<refStart> <refEnd>"},
		docoptOptions={""},
		metaTags={CmdMeta.updatesDatabase},
		description="Set the variation's location") 
public class VariationSetLocationCommand extends VariationModeCommand<OkResult> {

	private static final String REF_END = "refEnd";
	private static final String REF_START = "refStart";
	
	private Integer refStart;
	private Integer refEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, true);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		if(refStart > refEnd) {
			throw new VariationException(Code.VARIATION_ENDPOINTS_REVERSED, 
					getRefSeqName(), getFeatureName(), getVariationName(), Integer.toString(refStart), Integer.toString(refEnd));
		}
		Variation variation = lookupVariation(cmdContext);
		variation.setRefStart(refStart);
		variation.setRefEnd(refEnd);
		cmdContext.commit();
		return new UpdateResult(Variation.class, 1);
	}

}
