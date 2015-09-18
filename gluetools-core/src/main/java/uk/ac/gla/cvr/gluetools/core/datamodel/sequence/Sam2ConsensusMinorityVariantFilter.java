package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class Sam2ConsensusMinorityVariantFilter extends MinorityVariantFilter<Sam2ConsensusSequenceObject>{

	
	public static final String MIN_QUALITY = "minQuality";
	public static final String MAX_ENTROPY = "maxEntropy";
	
	private static final char[] ntValues = {'A', 'C', 'G', 'T'};
	
	private Integer minQuality = null;
	private Double maxEntropy = null;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minQuality = PluginUtils.configureIntProperty(configElem, MIN_QUALITY, false);
		maxEntropy = PluginUtils.configureDoubleProperty(configElem, MAX_ENTROPY, false);
	}
	
	protected Integer getMinQuality() {
		return minQuality;
	}

	protected Double getMaxEntropy() {
		return maxEntropy;
	}

	@Override
	public List<NtMinorityVariant> getMinorityVariants(int ntIndex, Sam2ConsensusSequenceObject seqObj) {
		List<NtMinorityVariant> ntMinorityVariants = new ArrayList<NtMinorityVariant>();
		Double maxEntropy = getMaxEntropy();
		Integer minQuality = getMinQuality();
		Double minProportion = getMinProportion();
		if(maxEntropy == null || seqObj.getEntropy(ntIndex) <= maxEntropy) {
			char consensus = seqObj.getConsensus(ntIndex);
			for(char ntValue: ntValues) {
				if(ntValue == consensus) {
					continue;
				}
				double proportion = seqObj.getProportion(ntIndex, ntValue);
				if(minProportion != null && proportion < minProportion) {
					continue;
				}
				if(minQuality != null && seqObj.getQuality(ntIndex, ntValue) < minQuality) {
					continue;
				}
				ntMinorityVariants.add(new NtMinorityVariant(ntIndex, ntValue, proportion));
			}
		}
		return ntMinorityVariants;
	}

}
