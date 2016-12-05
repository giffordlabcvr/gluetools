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
