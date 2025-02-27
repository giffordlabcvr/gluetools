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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class AlignmentShowFeatureCoverageResult extends BaseTableResult<MemberFeatureCoverage> {

	public static final String 
		ALIGNMENT_NAME = "alignmentName",
		SOURCE_NAME = "sourceName",
		SEQUENCE_ID = "sequenceID",
		REFERENCE_NT_COVERAGE = "referenceNtCoverage";


	public AlignmentShowFeatureCoverageResult(List<MemberFeatureCoverage> rowData) {
		super("alignmentShowMemberFeatureCoverageResult", rowData,
				column(ALIGNMENT_NAME, mfc -> mfc.getAlignmentMember().getAlignment().getName()),
				column(SOURCE_NAME, mfc -> mfc.getAlignmentMember().getSequence().getSource().getName()),
				column(SEQUENCE_ID, mfc -> mfc.getAlignmentMember().getSequence().getSequenceID()),
				column(REFERENCE_NT_COVERAGE, mfc -> mfc.getFeatureReferenceNtCoverage()));
	}

}
