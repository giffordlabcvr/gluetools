package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class ReferenceDiffNtMask {

	@PojoResultField
	public String refName;

	public char[] maskChars;
	
	/**
	 * "-" = no NT difference from reference.
	 * "X" = NT difference from reference.
	 * "I" = nothing to compare with (insertion relative to reference)
	 */
	@PojoResultField
	public String mask;

}
