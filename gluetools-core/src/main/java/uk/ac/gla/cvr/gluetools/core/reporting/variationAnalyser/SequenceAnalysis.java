package uk.ac.gla.cvr.gluetools.core.reporting.variationAnalyser;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class SequenceAnalysis {

	@PojoResultField
	public String fastaId;

	@PojoResultField
	public String targetRefName;

	
	public SequenceAnalysis(String fastaId, String targetRefName) {
		this.fastaId = fastaId;
		this.targetRefName = targetRefName;
	}

}
