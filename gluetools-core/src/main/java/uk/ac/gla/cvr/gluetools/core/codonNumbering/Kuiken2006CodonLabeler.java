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

import java.util.List;

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
	private String rootReferenceName;
	private String linkingAlignmentName;
	
	public Kuiken2006CodonLabeler() {
		super();
		addSimplePropertyName(ROOT_REFERENCE_NAME);
		addSimplePropertyName(LINKING_ALIGNMENT_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		rootReferenceName = PluginUtils.configureStringProperty(configElem, ROOT_REFERENCE_NAME, true);
		linkingAlignmentName = PluginUtils.configureStringProperty(configElem, LINKING_ALIGNMENT_NAME, true);
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

		
		
		LabeledCodon[] featureRefTcrIdxToCodon = new LabeledCodon[featureRefLabeledCodons.size()];
		for(LabeledCodon featureRefCodon: featureRefLabeledCodons) {
			featureRefTcrIdxToCodon[featureRefCodon.getTranscriptionIndex()] = featureRefCodon;
		}
		TIntObjectMap<LabeledCodon> rootRefNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();
		for(LabeledCodon labeledCodon: rootRefFeatureLoc.getLabeledCodons(cmdContext)) {
			for(ReferenceSegment refSeg: labeledCodon.getLcRefSegments()) {
				for(int refNt = refSeg.getRefStart(); refNt <= refSeg.getRefEnd(); refNt++) {
					rootRefNtToLabeledCodon.put(refNt, labeledCodon);
				}
			}
		}
				

		LabeledCodon initialFeatureRefCodon = featureRefTcrIdxToCodon[0];
		LabeledCodon finalFeatureRefCodon = featureRefTcrIdxToCodon[featureRefTcrIdxToCodon.length-1];

		// initial and final codons must have homologous counterparts on the root reference.
		LabeledCodon initialRootRefCodon = findRootRefCodon(featureRef, rootRef, feature, featureRefToRootRefSegs, rootRefNtToLabeledCodon,
				initialFeatureRefCodon, true, "Initial codon");
		initialFeatureRefCodon.setCodonLabel(initialRootRefCodon.getCodonLabel());

		LabeledCodon finalRootRefCodon = findRootRefCodon(featureRef, rootRef, feature, featureRefToRootRefSegs, rootRefNtToLabeledCodon,
				finalFeatureRefCodon, true, "Final codon");
		finalFeatureRefCodon.setCodonLabel(finalRootRefCodon.getCodonLabel());

		LabeledCodon lastMatchedRootRefCodon = initialRootRefCodon;
		int codonsSinceMatch = 0;
		for(int featureRefTcrIdx = 1; featureRefTcrIdx < finalFeatureRefCodon.getTranscriptionIndex(); featureRefTcrIdx++) {
			LabeledCodon featureRefCodon = featureRefTcrIdxToCodon[featureRefTcrIdx];
			LabeledCodon matchedRootRefCodon = findRootRefCodon(featureRef, rootRef, feature, featureRefToRootRefSegs, rootRefNtToLabeledCodon,
					featureRefCodon, false, "Codon");
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
			LabeledCodon featureRefCodon, boolean strict, String codonDesc) {
		// qaSegs mapping the featureRef codon's segments to themselves.
		List<LabeledCodonReferenceSegment> featureRefCodonLcSegments = featureRefCodon.getLcRefSegments();
		List<QueryAlignedSegment> featureRefCodonQaSegs = ReferenceSegment.asQueryAlignedSegments(featureRefCodonLcSegments);

		// qaSegs mapping the featureRef codon's segments to the root ref.
		List<QueryAlignedSegment> featureRefCodonInRootRefSegs = 
				QueryAlignedSegment.translateSegments(featureRefCodonQaSegs, featureRefToRootRefSegs);
		
		if(featureRefCodonInRootRefSegs.isEmpty()) {
			if(strict) {
				throw new Kuiken2006CodonLabelerException(Code.MAPPING_ERROR, rootRef.getName(), featureRef.getName(), feature.getName(), featureRefCodonLcSegments.toString(), 
						codonDesc+" does not have any homology to the root reference");
			} else {
				return null;
			}
		}
		
		LabeledCodon matchingRootRefCodon = rootRefNtToLabeledCodon.get(featureRefCodonInRootRefSegs.get(0).getRefStart());
		if(matchingRootRefCodon == null) {
			throw new Kuiken2006CodonLabelerException(Code.MAPPING_ERROR, rootRef.getName(), featureRef.getName(), feature.getName(), featureRefCodonLcSegments.toString(), 
					codonDesc+" has homology to the the root reference "+featureRefCodonInRootRefSegs+" but is not homologous to any root reference codon");
		}
		
		if(!ReferenceSegment.sameRegion(matchingRootRefCodon.getLcRefSegments(), featureRefCodonInRootRefSegs)) {
			throw new Kuiken2006CodonLabelerException(Code.MAPPING_ERROR, rootRef.getName(), featureRef.getName(), feature.getName(), featureRefCodonLcSegments.toString(), 
					codonDesc+" matches some coordinate of codon "+matchingRootRefCodon.getCodonLabel()+" on the root reference ("+featureRefCodonInRootRefSegs.get(0).getRefStart()+") but not the correct region -- "+
							featureRefCodonInRootRefSegs+" rather than "+matchingRootRefCodon.getLcRefSegments());
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
