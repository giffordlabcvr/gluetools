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

import java.util.Map;

public class MemberFeaturePresence {

	private Map<String,String> memberPkMap;
	private Map<String,String> featureLocationPkMap;
	private Double referenceNtCoverage;
	
	public MemberFeaturePresence(Map<String, String> memberPkMap,
			Map<String, String> featureLocationPkMap, Double referenceNtCoverage) {
		super();
		this.memberPkMap = memberPkMap;
		this.featureLocationPkMap = featureLocationPkMap;
		this.referenceNtCoverage = referenceNtCoverage;
	}

	public Map<String, String> getMemberPkMap() {
		return memberPkMap;
	}

	public Map<String, String> getFeatureLocationPkMap() {
		return featureLocationPkMap;
	}

	public Double getReferenceNtCoverage() {
		return referenceNtCoverage;
	}
}
