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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class MemberQueryAlignedSegmentsTableResult extends BaseTableResult<QueryAlignedSegment> {

	public static final String 
		MEMBER_NT_START = "memberNtStart",
		MEMBER_NT_END = "memberNtEnd",
		REL_REF_NT_START = "relRefNtStart",
		REL_REF_NT_END = "relRefNtEnd";


	public MemberQueryAlignedSegmentsTableResult(List<QueryAlignedSegment> rowData) {
		super("memberShowFeatureSegmentsResult", 
				rowData, 
				column(MEMBER_NT_START, qas -> qas.getQueryStart()),
				column(MEMBER_NT_END, qas -> qas.getQueryEnd()),
				column(REL_REF_NT_START, qas -> qas.getRefStart()),
				column(REL_REF_NT_END, qas -> qas.getRefEnd()));
	}


}
