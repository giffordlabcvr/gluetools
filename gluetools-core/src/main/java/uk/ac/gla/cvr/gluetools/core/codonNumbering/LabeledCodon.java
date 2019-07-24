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
import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public abstract class LabeledCodon {

	private String featureName;
	private String codonLabel;
	private int transcriptionIndex;
	private List<LabeledCodonReferenceSegment> lcRefSegs;
	private List<Integer> dependentRefNts;
	private int ntStart, ntEnd;
	
	
	protected LabeledCodon(String featureName, String codonLabel, List<Integer> dependentRefNts, int transcriptionIndex) {
		super();
		this.featureName = featureName;
		this.codonLabel = codonLabel;
		this.dependentRefNts = dependentRefNts;
		this.transcriptionIndex = transcriptionIndex;
		this.dependentRefNts.sort(null);
		this.ntStart = this.dependentRefNts.get(0);
		this.ntEnd = this.dependentRefNts.get(this.dependentRefNts.size()-1);
	}

	public String getFeatureName() {
		return featureName;
	}

	public String getCodonLabel() {
		return codonLabel;
	}

	public void setCodonLabel(String codonLabel) {
		this.codonLabel = codonLabel;
	}

	public int getTranscriptionIndex() {
		return transcriptionIndex;
	}

	public final int getNtStart() {
		return ntStart;
	}
	
	public final int getNtEnd() {
		return ntEnd;
	}

	public final int getNtLength() {
		return (getNtEnd() - getNtStart())+1;
	}
	
	public List<LabeledCodonReferenceSegment> getLcRefSegments() {
		if(lcRefSegs == null) {
			if(dependentRefNts.size() == 3 && ntStart == dependentRefNts.get(1)-1 && ntEnd == dependentRefNts.get(1)+1) {
				// simple case
				lcRefSegs = Arrays.asList(new LabeledCodonReferenceSegment(this, ntStart, ntEnd));
			} else {
				// point of this is because ntStart/ntMiddle/ntEnd may not be consecutive,
				// e.g. codons which span the introns region of a spliced gene.
				// also codon may depend on fewer than 3 query nucleotides, e.g. due to ribosomal slippage or RNA editing
				lcRefSegs = new ArrayList<LabeledCodonReferenceSegment>();
				for(Integer dependentRefNt: dependentRefNts) {
					lcRefSegs.add(new LabeledCodonReferenceSegment(this, dependentRefNt, dependentRefNt));
				}
				
				lcRefSegs = ReferenceSegment.mergeAbutting(lcRefSegs,
						LabeledCodonReferenceSegment.mergeAbuttingFunctionLabeledCodonReferenceSegment(), 
						ReferenceSegment.abutsPredicateReferenceSegment());
				ReferenceSegment.sortByRefStart(lcRefSegs);
			}
		}
		return lcRefSegs;
	}
}
