/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.Kuiken2006CodonLabelerException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@PluginClass(elemName="kuiken2006CodonLabeler",
		description="Facilitates the labelling of codon positions according to a scheme suggested for HCV in Kuiken et, al. 2006")
public class Kuiken2006CodonLabeler extends ModulePlugin<Kuiken2006CodonLabeler> implements CodonLabeler {

	private static final String ROOT_REFERENCE_NAME = "rootReferenceName";
	private static final String LINKING_ALIGNMENT_NAME = "linkingAlignmentName";
	private static final String ALLOW_UPSTREAM_LABELLING = "allowUpstreamLabelling";
	private static final String ALLOW_DOWNSTREAM_LABELLING = "allowDownstreamLabelling";
	private static final String ALLOW_UNMAPPED_LABELLING = "allowUnmappedLabelling";
	private String rootReferenceName;
	private String linkingAlignmentName;
	private boolean allowUpstreamLabelling;
	private boolean allowDownstreamLabelling;
	private boolean allowUnmappedLabelling;
	
	public Kuiken2006CodonLabeler() {
		super();
		addSimplePropertyName(ROOT_REFERENCE_NAME);
		addSimplePropertyName(LINKING_ALIGNMENT_NAME);
		addSimplePropertyName(ALLOW_UPSTREAM_LABELLING);
		addSimplePropertyName(ALLOW_DOWNSTREAM_LABELLING);
		addSimplePropertyName(ALLOW_UNMAPPED_LABELLING);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		rootReferenceName = PluginUtils.configureStringProperty(configElem, ROOT_REFERENCE_NAME, true);
		linkingAlignmentName = PluginUtils.configureStringProperty(configElem, LINKING_ALIGNMENT_NAME, true);
		allowUpstreamLabelling = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_UPSTREAM_LABELLING, false)).orElse(false);
		allowDownstreamLabelling = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_DOWNSTREAM_LABELLING, false)).orElse(false);
		allowUnmappedLabelling = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_UNMAPPED_LABELLING, false)).orElse(false);
	}

	
	
	@Override
	public void relabelCodons(CommandContext cmdContext, FeatureLocation featureRefFeatureLoc, List<LabeledCodon> featureRefLabeledCodons) {
		ReferenceSequence featureRef = featureRefFeatureLoc.getReferenceSequence();
		if(featureRef.getName().equals(rootReferenceName)) {
			return; // don't relabel root ref codons.
		}
		
		AlignmentMember linkingAlmtMember = featureRef.getLinkingAlignmentMembership(linkingAlignmentName);

		Alignment linkingAlignment = linkingAlmtMember.getAlignment();
		if(linkingAlignment.isConstrained()) {
			throw new Kuiken2006CodonLabelerException(Code.LINKING_ALIGNMENT_MUST_BE_UNCONSTRAINED, linkingAlignmentName);
		}
		ReferenceSequence rootRef = linkingAlignment.getRelatedRef(cmdContext, rootReferenceName);

		Feature feature = featureRefFeatureLoc.getFeature();
		FeatureLocation rootRefFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(rootRef.getName(), feature.getName()));

		List<QueryAlignedSegment> featureRefToLinkingAlmtSegs = linkingAlmtMember.segmentsAsQueryAlignedSegments();

		List<QueryAlignedSegment> featureRefToRootRefSegs =
				linkingAlignment.translateToRelatedRef(cmdContext, featureRefToLinkingAlmtSegs, rootRef);

		
		
		LabeledCodon[] featureRefTrlnIdxToCodon = new LabeledCodon[featureRefLabeledCodons.size()];
		for(LabeledCodon featureRefCodon: featureRefLabeledCodons) {
			featureRefTrlnIdxToCodon[featureRefCodon.getTranslationIndex()] = featureRefCodon;
		}
		TIntObjectMap<LabeledCodon> rootRefNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();
		List<LabeledCodon> rootRefLabeledCodons = rootRefFeatureLoc.getLabeledCodons(cmdContext);
		// actual first labeled codon on the root ref
		LabeledCodon firstRootRefLabeledCodon = rootRefLabeledCodons.get(0);
		// actual last labeled codon on the root ref
		LabeledCodon lastRootRefLabeledCodon = rootRefLabeledCodons.get(rootRefLabeledCodons.size()-1);
		
		for(LabeledCodon labeledCodon: rootRefLabeledCodons) {
			for(ReferenceSegment refSeg: labeledCodon.getLcRefSegments()) {
				for(int refNt = refSeg.getRefStart(); refNt <= refSeg.getRefEnd(); refNt++) {
					rootRefNtToLabeledCodon.put(refNt, labeledCodon);
				}
			}
		}
				

		// first feature ref codon with counterpart on the root reference
		LabeledCodon initialFeatureRefCodon = featureRefTrlnIdxToCodon[0];
		int initialFeatureRefCodonIdx = 0;

		// last feature ref codon with counterpart on the root reference
		LabeledCodon finalFeatureRefCodon = featureRefTrlnIdxToCodon[featureRefTrlnIdxToCodon.length-1];
		int finalFeatureRefCodonIdx = featureRefTrlnIdxToCodon.length-1;
		
		// counterpart on the root reference of initialFeatureRefCodon
		LabeledCodon initialRootRefCodon = null;

		// counterpart on the root reference of finalFeatureRefCodon
		LabeledCodon finalRootRefCodon = null;

		if(allowUpstreamLabelling) {
			List<LabeledCodon> upstreamFeatureRefCodons = new LinkedList<LabeledCodon>();
			for(initialFeatureRefCodonIdx = 0; initialFeatureRefCodonIdx < featureRefTrlnIdxToCodon.length; initialFeatureRefCodonIdx++) {
				initialFeatureRefCodon = featureRefTrlnIdxToCodon[initialFeatureRefCodonIdx];
				initialRootRefCodon = findRootRefCodon(featureRef, rootRef, feature, featureRefToRootRefSegs, rootRefNtToLabeledCodon,
						initialFeatureRefCodon, false, false, true, "Initial codon");
				if(initialRootRefCodon != null && initialRootRefCodon.getCodonLabel().equals(firstRootRefLabeledCodon.getCodonLabel())) {
					initialFeatureRefCodon.setCodonLabel(initialRootRefCodon.getCodonLabel());
					int upstreamIdx = 1;
					for(LabeledCodon labeledCodon: upstreamFeatureRefCodons) {
						labeledCodon.setCodonLabel("U"+upstreamIdx);
						upstreamIdx++;
					}
					break;
				} else {
					upstreamFeatureRefCodons.add(0, initialFeatureRefCodon);
				}
			}
			if(initialRootRefCodon == null) {
				throw new Kuiken2006CodonLabelerException(Code.MAPPING_ERROR_NO_LOCATIONS, rootRef.getName(), featureRef.getName(), feature.getName(), 
						"No labeled codons have counterparts on the root reference");
			}
		} else {
			// first codon on feature ref must have homologous counterpart on the root reference.
			initialRootRefCodon = findRootRefCodon(featureRef, rootRef, feature, featureRefToRootRefSegs, rootRefNtToLabeledCodon,
					initialFeatureRefCodon, true, true, true, "Initial codon");
			initialFeatureRefCodon.setCodonLabel(initialRootRefCodon.getCodonLabel());
			
		}

		if(allowDownstreamLabelling) {
			List<LabeledCodon> downstreamFeatureRefCodons = new LinkedList<LabeledCodon>();
			// count back from the end of the feature ref, trying to find the final codon of the root ref.
			// if you find it, label all subsequent feature ref codons using the Dxx style.
			for(finalFeatureRefCodonIdx = featureRefTrlnIdxToCodon.length-1; finalFeatureRefCodonIdx > initialFeatureRefCodonIdx; finalFeatureRefCodonIdx--) {
				finalFeatureRefCodon = featureRefTrlnIdxToCodon[finalFeatureRefCodonIdx];
				finalRootRefCodon = findRootRefCodon(featureRef, rootRef, feature, featureRefToRootRefSegs, rootRefNtToLabeledCodon,
						finalFeatureRefCodon, false, false, true, "Final codon");
				if(finalRootRefCodon != null && finalRootRefCodon.getCodonLabel().equals(lastRootRefLabeledCodon.getCodonLabel())) {
					finalFeatureRefCodon.setCodonLabel(finalRootRefCodon.getCodonLabel());
					int downstreamIdx = 1;
					for(LabeledCodon labeledCodon: downstreamFeatureRefCodons) {
						labeledCodon.setCodonLabel("D"+downstreamIdx);
						downstreamIdx++;	
					}
					break;
				} else {
					downstreamFeatureRefCodons.add(0, finalFeatureRefCodon);
				}
			}
		} else {
			finalRootRefCodon = findRootRefCodon(featureRef, rootRef, feature, featureRefToRootRefSegs, rootRefNtToLabeledCodon,
					finalFeatureRefCodon, true, true, true, "Final codon");
			finalFeatureRefCodon.setCodonLabel(finalRootRefCodon.getCodonLabel());
			
		}
		

		LabeledCodon lastMatchedRootRefCodon = initialRootRefCodon;
		int codonsSinceMatch = 0;
		for(int featureRefTcrIdx = initialFeatureRefCodonIdx+1; featureRefTcrIdx < finalFeatureRefCodonIdx; featureRefTcrIdx++) {
			LabeledCodon featureRefCodon = featureRefTrlnIdxToCodon[featureRefTcrIdx];
			LabeledCodon matchedRootRefCodon = findRootRefCodon(featureRef, rootRef, feature, featureRefToRootRefSegs, rootRefNtToLabeledCodon,
					featureRefCodon, false, true, !allowUnmappedLabelling, "Codon");
			if(matchedRootRefCodon == null) {
				codonsSinceMatch++;
				featureRefCodon.setCodonLabel(lastMatchedRootRefCodon.getCodonLabel()+getAlpha(codonsSinceMatch));
			} else {
				featureRefCodon.setCodonLabel(matchedRootRefCodon.getCodonLabel());
				lastMatchedRootRefCodon = matchedRootRefCodon;
				codonsSinceMatch = 0;
			}
		}
	}

	private LabeledCodon findRootRefCodon(ReferenceSequence featureRef, ReferenceSequence rootRef, Feature feature,
			List<QueryAlignedSegment> featureRefToRootRefSegs, TIntObjectMap<LabeledCodon> rootRefNtToLabeledCodon,
			LabeledCodon featureRefCodon, boolean mustHaveHomology, boolean mustMatchCodon, boolean mustMatchExactly, String codonDesc) {
		// qaSegs mapping the featureRef codon's segments to themselves.
		List<LabeledCodonReferenceSegment> featureRefCodonLcSegments = featureRefCodon.getLcRefSegments();
		List<QueryAlignedSegment> featureRefCodonQaSegs = ReferenceSegment.asQueryAlignedSegments(featureRefCodonLcSegments);

		// qaSegs mapping the featureRef codon's segments to the root ref.
		List<QueryAlignedSegment> featureRefCodonInRootRefSegs = 
				QueryAlignedSegment.translateSegments(featureRefCodonQaSegs, featureRefToRootRefSegs);
		
		if(featureRefCodonInRootRefSegs.isEmpty()) {
			if(mustHaveHomology) {
				throw new Kuiken2006CodonLabelerException(Code.MAPPING_ERROR, rootRef.getName(), featureRef.getName(), feature.getName(), featureRefCodonLcSegments.toString(), 
						codonDesc+" does not have any homology to the root reference");
			} else {
				return null;
			}
		}
		
		
		LabeledCodon matchingRootRefCodon = rootRefNtToLabeledCodon.get(featureRefCodonInRootRefSegs.get(0).getRefStart());
		
		if(mustMatchCodon) {
			if(matchingRootRefCodon == null) {
				throw new Kuiken2006CodonLabelerException(Code.MAPPING_ERROR, rootRef.getName(), featureRef.getName(), feature.getName(), featureRefCodonLcSegments.toString(), 
						codonDesc+" has homology to the the root reference "+featureRefCodonInRootRefSegs+" but is not homologous to any root reference codon");
			}
		}
		if(matchingRootRefCodon != null) {
			if(!ReferenceSegment.sameRegion(matchingRootRefCodon.getLcRefSegments(), featureRefCodonInRootRefSegs)) {
				if(mustMatchExactly) {
					if(featureRefCodonInRootRefSegs.size() == 2) {
						// sometimes the feature ref has a deletion relative to the root ref, but although the size of the deletion is 
						// a multiple of 3, it is not codon aligned in the alignment. It may be unwise to force it to be codon aligned (as this
						// would change the phylogenetics) or impossible (because of overlapping coding regions in different reading frames).
						// The upshot is that 2NTs of the query codon maps to one reference codon and the other NT maps to a different reference
						// codon. Pragmatically, we will label this according to the majority verdict.
						QueryAlignedSegment s1 = featureRefCodonInRootRefSegs.get(0);
						QueryAlignedSegment s2 = featureRefCodonInRootRefSegs.get(1);
						if( ( s2.getRefStart() - (s1.getRefEnd() + 1) ) % 3 == 0) { // deletion is multiple of 3.
							Map<String, Integer> matchingCodonLabelToCount = new LinkedHashMap<String, Integer>();
							for(int i = 0; i < s1.getCurrentLength(); i++) {
								LabeledCodon matchingCodon = rootRefNtToLabeledCodon.get(s1.getRefStart()+i);
								if(matchingCodon != null) {
									Integer count = matchingCodonLabelToCount.get(matchingCodon.getCodonLabel());
									if(count == null) {
										count = 1;
									} else {
										count = count+1;
									}
									if(count.intValue() == 2) {
										return matchingCodon;
									}
									matchingCodonLabelToCount.put(matchingCodon.getCodonLabel(), count);
								}
							}
							for(int i = 0; i < s2.getCurrentLength(); i++) {
								LabeledCodon matchingCodon = rootRefNtToLabeledCodon.get(s2.getRefStart()+i);
								if(matchingCodon != null) {
									Integer count = matchingCodonLabelToCount.get(matchingCodon.getCodonLabel());
									if(count == null) {
										count = 1;
									} else {
										count = count+1;
									}
									if(count.intValue() == 2) {
										return matchingCodon;
									}
									matchingCodonLabelToCount.put(matchingCodon.getCodonLabel(), count);
								}
							}
							
						}
						
					}
					if(mustMatchCodon) {
						throw new Kuiken2006CodonLabelerException(Code.MAPPING_ERROR, rootRef.getName(), featureRef.getName(), feature.getName(), featureRefCodonLcSegments.toString(), 
								codonDesc+" matches some coordinate of codon "+matchingRootRefCodon.getCodonLabel()+" on the root reference ("+featureRefCodonInRootRefSegs.get(0).getRefStart()+") but not the correct region -- "+
										featureRefCodonInRootRefSegs+" rather than "+matchingRootRefCodon.getLcRefSegments());
					} else {
						matchingRootRefCodon = null;
					}
				} else {
					matchingRootRefCodon = null;
				}
			}
		}
		
		return matchingRootRefCodon;
	}

	private String getAlpha(int num) {
		String result = "";
		while (num > 0) {
			num--; // 1 => a, not 0 => a
			int remainder = num % 26;
			char digit = (char) (remainder + 97);
			result = digit + result;
			num = (num - remainder) / 26;
		}
		return result;
	}


	public String getRootReferenceName() {
		return rootReferenceName;
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		ReferenceSequence rootRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(rootReferenceName));
		Alignment linkingAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(linkingAlignmentName));
		if(linkingAlignment.isConstrained()) {
			throw new Kuiken2006CodonLabelerException(Code.LINKING_ALIGNMENT_MUST_BE_UNCONSTRAINED, linkingAlignmentName);
		}
		rootRef.getLinkingAlignmentMembership(linkingAlignmentName);
	}

	
	
}
