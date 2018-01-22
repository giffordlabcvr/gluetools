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

import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;

public class FeaturePresence {

	private FeatureLocation featureLoc;
	private Integer membersWherePresent;
	private Integer totalMembers;
	
	public FeaturePresence(FeatureLocation featureLoc, Integer membersWherePresent, Integer totalMembers) {
		super();
		this.featureLoc = featureLoc;
		this.membersWherePresent = membersWherePresent;
		this.totalMembers = totalMembers;
	}

	public FeatureLocation getFeatureLoc() {
		return featureLoc;
	}

	public Integer getMembersWherePresent() {
		return membersWherePresent;
	}

	public Double getPercentWherePresent() {
		if(totalMembers.equals(0)) {
			return null;
		}
		return ( membersWherePresent * 100.0) / new Double(totalMembers);
	}

	public Integer getTotalMembers() {
		return totalMembers;
	}
	
	public void incrementMembersWherePresent() {
		this.membersWherePresent++;
	}
}
