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
package uk.ac.gla.cvr.gluetools.core.featurePresenceRecorder;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;

public class RecordFeaturePresenceResult extends BaseTableResult<MemberFeaturePresence> {

	public static final String REFERENCE_NT_COVERAGE = "referenceNtCoverage";

	public RecordFeaturePresenceResult(List<MemberFeaturePresence> rowObjects) {
		super("recordFeaturePresenceResult", rowObjects, 
				column(MemberFLocNote.ALIGNMENT_NAME_PATH, 
						mfp -> mfp.getMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)), 
				column(MemberFLocNote.SOURCE_NAME_PATH, 
						mfp -> mfp.getMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)), 
				column(MemberFLocNote.SEQUENCE_ID_PATH, 
						mfp -> mfp.getMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH)), 
				column(MemberFLocNote.REF_SEQ_NAME_PATH, 
						mfp -> mfp.getFeatureLocationPkMap().get(FeatureLocation.REF_SEQ_NAME_PATH)), 
				column(MemberFLocNote.FEATURE_NAME_PATH, 
						mfp -> mfp.getFeatureLocationPkMap().get(FeatureLocation.FEATURE_NAME_PATH)), 
				column(REFERENCE_NT_COVERAGE, 
						mfp -> mfp.getReferenceNtCoverage()));
	}

}
