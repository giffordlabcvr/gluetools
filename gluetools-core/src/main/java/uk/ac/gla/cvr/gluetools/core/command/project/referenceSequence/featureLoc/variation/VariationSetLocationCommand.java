package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
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
		metaTags={CmdMeta.updatesDatabase},
		description="Set the variation's location", 
		furtherHelp="Note that the meaning of refStart and refEnd are dependent on the transcription type and on the feature location: "+
				"For variations of type NUCLEOTIDE, refStart and refEnd define simply the NT region of the reference sequence to "+
				"which the motif should be aligned. For variations of type AMINO_ACID, refStart and refEnd define the codon-numbered "+
				"region to which the query should be aligned, based on the numbering scheme of the smallest-scope feature ancestor of "+
				"this variation which has its own codon numbering.") 
public class VariationSetLocationCommand extends VariationModeCommand<OkResult> {

	public static final String REF_END = "refEnd";
	public static final String REF_START = "refStart";
	
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

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}

}
