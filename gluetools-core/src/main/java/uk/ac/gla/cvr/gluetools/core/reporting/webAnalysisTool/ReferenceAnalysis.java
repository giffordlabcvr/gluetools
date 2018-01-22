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
package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class ReferenceAnalysis {

	@PojoDocumentField
	public String refName;

	@PojoDocumentField
	public String sourceName;

	@PojoDocumentField
	public String sequenceID;
	
	@PojoDocumentField
	public String parentRefName;

	@PojoDocumentField
	public String containingAlmtName;

	@PojoDocumentListField(itemClass = SequenceFeatureAnalysis.class)
	public List<SequenceFeatureAnalysis<ReferenceAa, ReferenceNtSegment>> sequenceFeatureAnalysis;
	
	private ReferenceSequence refSeq;
	private Alignment containingAlmt;
	private AlignmentMember containingAlmtMember;
	
	public ReferenceAnalysis(ReferenceSequence refSeq, Alignment containingAlmt, AlignmentMember containingAlmtMember) {
		this.refSeq = refSeq;
		this.refName = refSeq.getName();
		this.sourceName = refSeq.getSequence().getSource().getName();
		this.sequenceID = refSeq.getSequence().getSequenceID();
		this.parentRefName = containingAlmt == null ? null : containingAlmt.getRefSequence().getName();
		this.containingAlmtMember = containingAlmtMember;
		this.containingAlmt = containingAlmt;
		this.containingAlmtName = containingAlmt == null ? null : containingAlmt.getName();
	}

	public ReferenceSequence getRefSeq() {
		return refSeq;
	}

	public Alignment getContainingAlmt() {
		return containingAlmt;
	}

	public AlignmentMember getContainingAlmtMember() {
		return containingAlmtMember;
	}

	public Optional<SequenceFeatureAnalysis<ReferenceAa, ReferenceNtSegment>> getSeqFeatAnalysis(String featureName) {
		return sequenceFeatureAnalysis.stream().filter(seqFeatAnalysis -> seqFeatAnalysis.featureName.equals(featureName)).findFirst();
	}
}
