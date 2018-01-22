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
package uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;

@GlueDataClass(
		defaultListedProperties = { MemberFLocNote.ALIGNMENT_NAME_PATH, MemberFLocNote.SOURCE_NAME_PATH, MemberFLocNote.SEQUENCE_ID_PATH, MemberFLocNote.REF_SEQ_NAME_PATH, MemberFLocNote.FEATURE_NAME_PATH },
		listableBuiltInProperties = { MemberFLocNote.ALIGNMENT_NAME_PATH, MemberFLocNote.SOURCE_NAME_PATH, MemberFLocNote.SEQUENCE_ID_PATH, MemberFLocNote.REF_SEQ_NAME_PATH, MemberFLocNote.FEATURE_NAME_PATH },
		modifiableBuiltInProperties = { })		
public class MemberFLocNote extends _MemberFLocNote {

	public static final String ALIGNMENT_NAME_PATH = _MemberFLocNote.MEMBER_PROPERTY+"."+AlignmentMember.ALIGNMENT_NAME_PATH;
	public static final String SOURCE_NAME_PATH = _MemberFLocNote.MEMBER_PROPERTY+"."+AlignmentMember.SOURCE_NAME_PATH;
	public static final String SEQUENCE_ID_PATH = _MemberFLocNote.MEMBER_PROPERTY+"."+AlignmentMember.SEQUENCE_ID_PATH;
	public static final String REF_SEQ_NAME_PATH = _MemberFLocNote.FEATURE_LOC_PROPERTY+"."+FeatureLocation.REF_SEQ_NAME_PATH;
	public static final String FEATURE_NAME_PATH  = _MemberFLocNote.FEATURE_LOC_PROPERTY+"."+FeatureLocation.FEATURE_NAME_PATH;

	public static Map<String, String> pkMap(
			String alignmentName, 
			String sourceName, 
			String sequenceID, 
			String referenceName, 
			String featureName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(SOURCE_NAME_PATH, sourceName);
		idMap.put(SEQUENCE_ID_PATH, sequenceID);
		idMap.put(REF_SEQ_NAME_PATH, referenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getMember().getAlignment().getName(), 
				getMember().getSequence().getSource().getName(),
				getMember().getSequence().getSequenceID(),
				getFeatureLoc().getReferenceSequence().getName(), 
				getFeatureLoc().getFeature().getName());
	}

	
}
