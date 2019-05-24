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

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class LabeledCodon {

	private String featureName;
	private String codonLabel;
	private int ntStart;
	private int ntMiddle;
	private int ntEnd;
	private int transcriptionIndex;
	private List<LabeledCodonReferenceSegment> lcRefSegs;
	
	
	public LabeledCodon(String featureName, String codonLabel, int ntStart, int ntMiddle, int ntEnd, int transcriptionIndex) {
		super();
		this.featureName = featureName;
		this.codonLabel = codonLabel;
		this.ntStart = ntStart;
		this.ntMiddle = ntMiddle;
		this.ntEnd = ntEnd;
		this.transcriptionIndex = transcriptionIndex;
	}

	public String getFeatureName() {
		return featureName;
	}

	public String getCodonLabel() {
		return codonLabel;
	}

	public int getNtStart() {
		return ntStart;
	}
	
	public int getNtMiddle() {
		return ntMiddle;
	}

	public int getNtEnd() {
		return ntEnd;
	}

	public int getTranscriptionIndex() {
		return transcriptionIndex;
	}

	public int getNtLength() {
		return 3;
	}
	
	public List<LabeledCodonReferenceSegment> getLcRefSegments() {
		if(lcRefSegs == null) {
			lcRefSegs = new ArrayList<LabeledCodonReferenceSegment>();
			lcRefSegs.add(new LabeledCodonReferenceSegment(this, ntStart, ntStart));
			lcRefSegs.add(new LabeledCodonReferenceSegment(this, ntMiddle, ntMiddle));
			lcRefSegs.add(new LabeledCodonReferenceSegment(this, ntEnd, ntEnd));
			
			lcRefSegs = ReferenceSegment.mergeAbutting(lcRefSegs,
				LabeledCodonReferenceSegment.mergeAbuttingFunctionLabeledCodonReferenceSegment(), 
				ReferenceSegment.abutsPredicateReferenceSegment());
		}
		return lcRefSegs;
	}
}
