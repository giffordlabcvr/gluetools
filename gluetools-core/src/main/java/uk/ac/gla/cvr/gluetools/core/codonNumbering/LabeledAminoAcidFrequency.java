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

public class LabeledAminoAcidFrequency {

	private LabeledCodon labeledCodon;
	private String aminoAcid;
	
	private int numMembers;
	private int totalMembers;
	private double pctMembers;
	
	public LabeledAminoAcidFrequency(LabeledCodon labeledCodon, String aminoAcid,
			int numMembers, int totalMembers, double pctMembers) {
		super();
		this.labeledCodon = labeledCodon;
		this.aminoAcid = aminoAcid;
		this.numMembers = numMembers;
		this.totalMembers = totalMembers;
		this.pctMembers = pctMembers;
	}

	public LabeledCodon getLabeledCodon() {
		return labeledCodon;
	}

	public String getAminoAcid() {
		return aminoAcid;
	}

	public int getNumMembers() {
		return numMembers;
	}

	public int getTotalMembers() {
		return totalMembers;
	}

	public double getPctMembers() {
		return pctMembers;
	}
	
}
