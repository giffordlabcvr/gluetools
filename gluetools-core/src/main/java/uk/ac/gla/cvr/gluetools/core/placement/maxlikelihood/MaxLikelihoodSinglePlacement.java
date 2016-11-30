package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class MaxLikelihoodSinglePlacement {

	@PojoDocumentField
	public Integer edgeIndex;

	@PojoDocumentField
	public Double likeWeightRatio;

	@PojoDocumentField
	public Double logLikelihood;

	@PojoDocumentField
	public Double distalLength;

	@PojoDocumentField
	public Double pendantLength;

}
