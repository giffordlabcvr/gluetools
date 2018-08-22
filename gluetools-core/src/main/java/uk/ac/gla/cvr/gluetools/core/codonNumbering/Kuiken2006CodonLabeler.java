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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

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
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

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
	public List<LabeledCodon> labelCodons(CommandContext cmdContext, FeatureLocation featureLoc) {
		Integer ntStart = ReferenceSegment.minRefStart(featureLoc.getSegments());
		Integer ntEnd = ReferenceSegment.maxRefEnd(featureLoc.getSegments());

		Feature feature = featureLoc.getFeature();
		ReferenceSequence featureRefSeq = featureLoc.getReferenceSequence();

		Integer featureRefCodon1Start = featureLoc.getCodon1Start(cmdContext);
		Integer rootCodon1Start;

		AlignmentMember linkingAlmtMember = featureRefSeq.getLinkingAlignmentMembership(linkingAlignmentName);

		Alignment linkingAlignment = linkingAlmtMember.getAlignment();
		if(linkingAlignment.isConstrained()) {
			throw new Kuiken2006CodonLabelerException(Code.LINKING_ALIGNMENT_MUST_BE_UNCONSTRAINED, linkingAlignmentName);
		}

		ReferenceSequence rootReference = linkingAlignment.getRelatedRef(cmdContext, rootReferenceName);
		FeatureLocation rootFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(rootReference.getName(), feature.getName()));

		List<QueryAlignedSegment> featureRefToLinkingAlmtSegs = linkingAlmtMember.segmentsAsQueryAlignedSegments();

		List<QueryAlignedSegment> featureRefToRootRefSegs =
				linkingAlignment.translateToRelatedRef(cmdContext, featureRefToLinkingAlmtSegs, rootReference);

		List<QueryAlignedSegment> featureRefToRootRefFeatureSegs = 
				ReferenceSegment.intersection(featureRefToRootRefSegs, rootFeatureLoc.getSegments(), ReferenceSegment.cloneLeftSegMerger());


		rootCodon1Start = rootFeatureLoc.getCodon1Start(cmdContext);

		List<LabeledCodon> numberedCodons = new ArrayList<LabeledCodon>();

		Integer lastRootLocation = null;
		Integer lastFeatureRefLocation = null;
		QueryAlignedSegment currentSegment = null;


		if(featureRefToRootRefFeatureSegs.isEmpty() || featureRefToRootRefFeatureSegs.get(0).getQueryStart() > ntStart) {
			throw new Kuiken2006CodonLabelerException(Code.GAP_AT_START, rootReferenceName, ntStart);
		}

		for(int i = ntStart; i <= ntEnd; i++) {
			if(currentSegment != null) {
				if(i > currentSegment.getQueryEnd()) {
					currentSegment = null;
				} 
			}
			if(currentSegment == null && !featureRefToRootRefFeatureSegs.isEmpty()) {
				if(i >= featureRefToRootRefFeatureSegs.get(0).getQueryStart() &&
						i <= featureRefToRootRefFeatureSegs.get(0).getQueryEnd()) {
					currentSegment = featureRefToRootRefFeatureSegs.remove(0);
				}
			}
			if(currentSegment != null) {
				lastFeatureRefLocation = i;
				lastRootLocation = i+currentSegment.getQueryToReferenceOffset();
			}	
			LabeledCodon labeledCodon = null;
			if(currentSegment != null) {
				if(TranslationUtils.isAtStartOfCodon(featureRefCodon1Start, i)) {
					int rootCodon = TranslationUtils.getCodon(rootCodon1Start, 
							i+currentSegment.getQueryToReferenceOffset());
					labeledCodon = new LabeledCodon(Integer.toString(rootCodon), i, i+1, i+2);
				}
			} else {
				if(TranslationUtils.isAtStartOfCodon(featureRefCodon1Start, i)) {
					int diff = i-lastFeatureRefLocation;
					int lastRootCodon = TranslationUtils.getCodon(rootCodon1Start, lastRootLocation);
					int constrainingCodon = TranslationUtils.getCodon(rootCodon1Start, lastRootLocation+diff);
					String label = Integer.toString(lastRootCodon) + getAlpha(constrainingCodon - lastRootCodon);
					labeledCodon = new LabeledCodon(label, i, i+1, i+2);
				}
			}
			if(labeledCodon != null) {
				numberedCodons.add(labeledCodon);
			}
		}
		return numberedCodons;
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

	@Override
	public int compareCodonLabels(String codonLabel1, String codonLabel2) {
		Integer number1 = null;
		String alpha1 = null;
		Integer splitPoint1 = null;
		for(int i = 0; i < codonLabel1.length(); i++) {
			if(Character.isAlphabetic(codonLabel1.charAt(i))) {
				splitPoint1 = i;
			}
		}
		if(splitPoint1 != null) {
			number1 = Integer.parseInt(codonLabel1.substring(0, splitPoint1));
			alpha1 = codonLabel1.substring(splitPoint1);
		} else {
			number1 = Integer.parseInt(codonLabel1);
		}
		Integer number2 = null;
		String alpha2 = null;
		Integer splitPoint2 = null;
		for(int i = 0; i < codonLabel2.length(); i++) {
			if(Character.isAlphabetic(codonLabel2.charAt(i))) {
				splitPoint2 = i;
			}
		}
		if(splitPoint2 != null) {
			number2 = Integer.parseInt(codonLabel2.substring(0, splitPoint2));
			alpha2 = codonLabel2.substring(splitPoint2);
		} else {
			number2 = Integer.parseInt(codonLabel2);
		}
		int comp = Integer.compare(number1, number2);
		if(comp != 0) { return comp; }
		if(alpha1 == null && alpha2 == null) {
			return 0;
		}
		if(alpha1 == null && alpha2 != null) {
			return -1;
		}
		if(alpha1 != null && alpha2 == null) {
			return 1;
		}
		return alpha1.compareTo(alpha2);
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
