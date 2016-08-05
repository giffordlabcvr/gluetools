package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class TypingAnalysis {

	@PojoResultField
	public String typeAlignmentName;

	@PojoResultField
	public String typeAlignmentDisplayName;

	@PojoResultField
	public String summaryCode;

	@PojoResultField
	public String closestMemberAlignmentName;

	@PojoResultField
	public String closestMemberAlignmentDisplayName;

	@PojoResultField
	public String closestMemberSourceName;

	@PojoResultField
	public String closestMemberSequenceID;

	@PojoResultField
	public Double likeWeightRatio;

	@PojoResultField
	public Double distanceToClosestMember;

	
}
