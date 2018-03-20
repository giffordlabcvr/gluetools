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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import gnu.trove.map.TIntObjectMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.AmbigNtTripletInfo;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public abstract class FeatureLocBaseAminoAcidCommand<R extends CommandResult> extends FeatureLocModeCommand<R> {

	public static List<LabeledAminoAcid> featureLocAminoAcids(CommandContext cmdContext, FeatureLocation featureLoc) {
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();

		// feature area coordinates.
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		final Translator translator = new CommandContextTranslator(cmdContext);

		AbstractSequenceObject refSeqObj = featureLoc.getReferenceSequence().getSequence().getSequenceObject();

		if(featureLocRefSegs.isEmpty()) {
			return Collections.emptyList();
		}
		
		TIntObjectMap<LabeledCodon> refNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);

		List<LabeledAminoAcid> labeledAminoAcids = new ArrayList<LabeledAminoAcid>();

		featureLocRefSegs = ReferenceSegment.mergeAbutting(featureLocRefSegs, 
				ReferenceSegment.mergeAbuttingFunctionReferenceSegment(), ReferenceSegment.abutsPredicateReferenceSegment());
		
		for(ReferenceSegment featureLocRefSeg: featureLocRefSegs) {
			CharSequence nts = refSeqObj.subSequence(cmdContext, 
					featureLocRefSeg.getRefStart(), featureLocRefSeg.getRefEnd());
			List<AmbigNtTripletInfo> segTranslationInfos = translator.translate(nts);
			int refNt = featureLocRefSeg.getRefStart();
			for(int i = 0; i < segTranslationInfos.size(); i++) {
				AmbigNtTripletInfo segTranslationInfo = segTranslationInfos.get(i);
				LabeledCodon labeledCodon = refNtToLabeledCodon.get(refNt);
				labeledAminoAcids.add(new LabeledAminoAcid(labeledCodon, segTranslationInfo));
				refNt = refNt+3;
			}
		}
		return labeledAminoAcids;
	}

}
