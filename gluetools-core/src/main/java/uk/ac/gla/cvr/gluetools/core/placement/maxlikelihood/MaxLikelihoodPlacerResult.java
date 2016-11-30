package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class MaxLikelihoodPlacerResult {

	@PojoDocumentField
	public String labelledPhyloTree;
	
	@PojoDocumentField
	public String labelledPhyloTreeFormat;

	@PojoDocumentListField(itemClass = MaxLikelihoodSingleQueryResult.class)
	public List<MaxLikelihoodSingleQueryResult> singleQueryResult = new ArrayList<MaxLikelihoodSingleQueryResult>();
	
}
