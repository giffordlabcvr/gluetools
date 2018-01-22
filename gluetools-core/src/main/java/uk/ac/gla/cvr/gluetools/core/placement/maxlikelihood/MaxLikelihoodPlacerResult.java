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
package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;

@PojoDocumentClass
public class MaxLikelihoodPlacerResult {

	@PojoDocumentField
	public String labelledPhyloTree;
	
	@PojoDocumentField
	public String labelledPhyloTreeFormat;

	@PojoDocumentListField(itemClass = MaxLikelihoodSingleQueryResult.class)
	public List<MaxLikelihoodSingleQueryResult> singleQueryResult = new ArrayList<MaxLikelihoodSingleQueryResult>();

	public PhyloTree getLabelledPhyloTree() {
		PhyloFormat labelledPhyloTreeFormatEnum;
		try {
			labelledPhyloTreeFormatEnum = PhyloFormat.valueOf(labelledPhyloTreeFormat);
		} catch(Exception e) {
			throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
					e, "Failed to parse labelled phylo tree format: "+e.getLocalizedMessage());
		}
		try {
			return labelledPhyloTreeFormatEnum.parse(labelledPhyloTree.getBytes());
		} catch(Exception e) {
			throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
					e, "Failed to parse labelled phylo tree: "+e.getLocalizedMessage());
		}

	}
}
