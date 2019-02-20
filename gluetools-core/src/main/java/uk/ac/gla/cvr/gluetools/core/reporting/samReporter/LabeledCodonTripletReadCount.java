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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;

public class LabeledCodonTripletReadCount {
	
	private LabeledCodon labeledCodon;
	private String triplet;
	private String aminoAcid;
	private int samRefNt;
	private int readsWithTriplet;
	private int readsWithDifferentTriplet;
	private double percentReadsWithTriplet;
	
	public LabeledCodonTripletReadCount(LabeledCodon labeledCodon, String triplet, String aminoAcid,
			int samRefNt, int readsWithTriplet, int readsWithDifferentTriplet, double percentReadsWithTriplet) {
		super();
		this.labeledCodon = labeledCodon;
		this.triplet = triplet;
		this.aminoAcid = aminoAcid;
		this.samRefNt = samRefNt;
		this.readsWithTriplet = readsWithTriplet;
		this.percentReadsWithTriplet = percentReadsWithTriplet;
		this.readsWithDifferentTriplet = readsWithDifferentTriplet;
	}

	public LabeledCodon getLabeledCodon() {
		return labeledCodon;
	}

	public String getAminoAcid() {
		return aminoAcid;
	}

	public String getTriplet() {
		return triplet;
	}

	public int getReadsWithTriplet() {
		return readsWithTriplet;
	}

	public double getPercentReadsWithTriplet() {
		return percentReadsWithTriplet;
	}

	public int getSamRefNt() {
		return samRefNt;
	}

	public int getReadsWithDifferentTriplet() {
		return readsWithDifferentTriplet;
	}
	
	
	
}