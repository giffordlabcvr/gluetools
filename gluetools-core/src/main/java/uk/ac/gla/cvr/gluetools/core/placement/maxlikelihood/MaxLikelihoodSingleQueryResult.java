package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class MaxLikelihoodSingleQueryResult {

	@PojoDocumentField
	public String queryName;
	
	@PojoDocumentListField(itemClass = MaxLikelihoodSinglePlacement.class)
	public List<MaxLikelihoodSinglePlacement> singlePlacement = new ArrayList<MaxLikelihoodSinglePlacement>();

	
}
