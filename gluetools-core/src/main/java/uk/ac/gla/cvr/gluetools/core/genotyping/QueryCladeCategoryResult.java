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
package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class QueryCladeCategoryResult {

	@PojoDocumentField
	public String categoryName;

	@PojoDocumentField
	public String categoryDisplayName;

	@PojoDocumentListField(itemClass = QueryCladeResult.class)
	public List<QueryCladeResult> queryCladeResult = new ArrayList<QueryCladeResult>();
	
	@PojoDocumentField
	public String finalClade;

	@PojoDocumentField
	public String finalCladeRenderedName;

	// closest member of the final clade to the query.
	@PojoDocumentField
	public String closestMemberAlignmentName;
	
	@PojoDocumentField
	public String closestMemberSourceName;

	@PojoDocumentField
	public String closestMemberSequenceID;

	// closest phylo leaf which also passes the "valid target" where clause
	@PojoDocumentField
	public String closestTargetAlignmentName;
	
	@PojoDocumentField
	public String closestTargetSourceName;

	@PojoDocumentField
	public String closestTargetSequenceID;


}
