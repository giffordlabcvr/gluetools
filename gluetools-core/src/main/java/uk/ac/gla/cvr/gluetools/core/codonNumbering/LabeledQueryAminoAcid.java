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


public class LabeledQueryAminoAcid {

	private LabeledAminoAcid labeledAminoAcid;
	private int queryNtStart;
	private int queryNtEnd;
	private List<Integer> dependentQueryPositions;
	
	public LabeledQueryAminoAcid(LabeledAminoAcid labeledAminoAcid, List<Integer> dependentQueryPositions) {
		this.labeledAminoAcid = labeledAminoAcid;
		this.dependentQueryPositions = dependentQueryPositions;
		this.queryNtStart = this.dependentQueryPositions.get(0);
		this.queryNtEnd = this.dependentQueryPositions.get(this.dependentQueryPositions.size()-1);
	}

	public LabeledAminoAcid getLabeledAminoAcid() {
		return labeledAminoAcid;
	}

	public int getQueryNtStart() {
		return queryNtStart;
	}

	public int getQueryNtEnd() {
		return queryNtEnd;
	}

	public static List<List<LabeledQueryAminoAcid>> findContiguousLqaaSections(
			List<LabeledQueryAminoAcid> lqaas) {
		List<List<LabeledQueryAminoAcid>> contiguousLqaaSections = new ArrayList<List<LabeledQueryAminoAcid>>();
		LabeledQueryAminoAcid lastLqaa = null;
		List<LabeledQueryAminoAcid> currentSection = null;
		for(LabeledQueryAminoAcid lqaa: lqaas) {
			if(lastLqaa == null) {
				currentSection = new ArrayList<LabeledQueryAminoAcid>();
				currentSection.add(lqaa);
				contiguousLqaaSections.add(currentSection);
			} else {
				if(lastLqaa.getQueryNtEnd() == lqaa.getQueryNtStart() - 1) {
					currentSection.add(lqaa);
				} else {
					currentSection = new ArrayList<LabeledQueryAminoAcid>();
					currentSection.add(lqaa);
					contiguousLqaaSections.add(currentSection);
				}
			}
			lastLqaa = lqaa;
		}
		return contiguousLqaaSections;
	}

}
