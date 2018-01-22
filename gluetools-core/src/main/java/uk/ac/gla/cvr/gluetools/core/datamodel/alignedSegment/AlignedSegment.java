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
package uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@GlueDataClass(defaultListedProperties = {
		_AlignedSegment.REF_START_PROPERTY, 
		_AlignedSegment.REF_END_PROPERTY, 
		_AlignedSegment.MEMBER_START_PROPERTY, 
		_AlignedSegment.MEMBER_END_PROPERTY})
public class AlignedSegment extends _AlignedSegment implements IQueryAlignedSegment {

	public static final String MEMBER_SOURCE_NAME_PATH = 
			_AlignedSegment.ALIGNMENT_MEMBER_PROPERTY+"."+_AlignmentMember.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String MEMBER_SEQUENCE_ID_PATH = 
			_AlignedSegment.ALIGNMENT_MEMBER_PROPERTY+"."+_AlignmentMember.SEQUENCE_PROPERTY+"."+
					_Sequence.SEQUENCE_ID_PROPERTY;

	public static final String ALIGNMENT_NAME_PATH = 
			_AlignedSegment.ALIGNMENT_MEMBER_PROPERTY+"."+_AlignmentMember.ALIGNMENT_PROPERTY+"."+
					_Alignment.NAME_PROPERTY;
	
	
	public static Map<String, String> pkMap(String alignmentName, 
			String memberSourceName, String memberSequenceID,
			int refStart, int refEnd, int memberStart, int memberEnd) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(MEMBER_SOURCE_NAME_PATH, memberSourceName);
		idMap.put(MEMBER_SEQUENCE_ID_PATH, memberSequenceID);
		idMap.put(REF_START_PROPERTY, Integer.toString(refStart));
		idMap.put(REF_END_PROPERTY, Integer.toString(refEnd));
		idMap.put(MEMBER_START_PROPERTY, Integer.toString(memberStart));
		idMap.put(MEMBER_END_PROPERTY, Integer.toString(memberEnd));
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setRefStart(Integer.parseInt(pkMap.get(REF_START_PROPERTY)));
		setRefEnd(Integer.parseInt(pkMap.get(REF_END_PROPERTY)));
		setMemberStart(Integer.parseInt(pkMap.get(MEMBER_START_PROPERTY)));
		setMemberEnd(Integer.parseInt(pkMap.get(MEMBER_END_PROPERTY)));
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(
				getAlignmentMember().getAlignment().getName(),
				getAlignmentMember().getSequence().getSource().getName(), 
				getAlignmentMember().getSequence().getSequenceID(),
				getRefStart(), 
				getRefEnd(), 
				getMemberStart(), 
				getMemberEnd());
	}
	
	public QueryAlignedSegment asQueryAlignedSegment() {
		return new QueryAlignedSegment(getRefStart(), getRefEnd(), getMemberStart(), getMemberEnd());
	}



	@Override
	public Integer getQueryStart() {
		return getMemberStart();
	}

	@Override
	public Integer getQueryEnd() {
		return getMemberEnd();
	}

	@Override
	public void setQueryStart(Integer queryStart) {
		setMemberStart(queryStart);
	}

	@Override
	public void setQueryEnd(Integer queryEnd) {
		setMemberEnd(queryEnd);
	}
	
	public AlignedSegment clone() {
		AlignedSegment copy = new AlignedSegment();
		copy.setRefStart(getRefStart());
		copy.setRefEnd(getRefEnd());
		copy.setMemberStart(getMemberStart());
		copy.setMemberEnd(getMemberEnd());
		return copy;
	}

}
