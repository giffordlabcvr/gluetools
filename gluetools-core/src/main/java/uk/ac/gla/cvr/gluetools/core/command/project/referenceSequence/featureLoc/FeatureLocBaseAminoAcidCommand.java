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
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.AmbigNtTripletInfo;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public abstract class FeatureLocBaseAminoAcidCommand<R extends CommandResult> extends FeatureLocModeCommand<R> {

	public static List<LabeledQueryAminoAcid> featureLocAminoAcids(CommandContext cmdContext, FeatureLocation featureLoc) {
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();

		// feature area coordinates.
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		final Translator translator = new CommandContextTranslator(cmdContext);

		AbstractSequenceObject refSeqObj = featureLoc.getReferenceSequence().getSequence().getSequenceObject();

		if(featureLocRefSegs.isEmpty()) {
			return Collections.emptyList();
		}
		
		featureLocRefSegs = ReferenceSegment.mergeAbutting(featureLocRefSegs, 
				ReferenceSegment.mergeAbuttingFunctionReferenceSegment(), ReferenceSegment.abutsPredicateReferenceSegment());


		TIntObjectMap<LabeledCodon> refNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);
		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = new ArrayList<LabeledQueryAminoAcid>();

		int segIndex = 0;
		ReferenceSegment currentSegment = featureLocRefSegs.get(segIndex);
		int refNt = currentSegment.getRefStart();
		while(segIndex < featureLocRefSegs.size() && refNt <= currentSegment.getRefEnd()) {
			LabeledCodon labeledCodon = refNtToLabeledCodon.get(refNt);
			if(labeledCodon != null) {
				char[] nts = new char[3];
				nts[0] = refSeqObj.nt(cmdContext, labeledCodon.getNtStart());
				nts[1] = refSeqObj.nt(cmdContext, labeledCodon.getNtMiddle());
				nts[2] = refSeqObj.nt(cmdContext, labeledCodon.getNtEnd());
				AmbigNtTripletInfo ambigNtTripletInfo = translator.translate(new String(nts)).get(0);
				LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(labeledCodon, ambigNtTripletInfo);
				labeledQueryAminoAcids.add(new LabeledQueryAminoAcid(labeledAminoAcid, refNt));
			}
			refNt++;
			if(refNt > currentSegment.getRefEnd()) {
				segIndex++;
				if(segIndex < featureLocRefSegs.size()) {
					currentSegment = featureLocRefSegs.get(segIndex);
					refNt = currentSegment.getRefStart();
				}
			}
		}
		return labeledQueryAminoAcids;
	}

}
