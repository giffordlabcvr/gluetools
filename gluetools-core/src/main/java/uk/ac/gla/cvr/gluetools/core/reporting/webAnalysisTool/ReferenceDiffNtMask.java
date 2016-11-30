package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class ReferenceDiffNtMask {

	@PojoDocumentField
	public String refName;

	public char[] maskChars;
	
	/**
	 * "-" = no NT difference from reference.
	 * "X" = NT difference from reference.
	 * "I" = nothing to compare with (insertion relative to reference)
	 */
	@PojoDocumentField
	public String mask;

}
