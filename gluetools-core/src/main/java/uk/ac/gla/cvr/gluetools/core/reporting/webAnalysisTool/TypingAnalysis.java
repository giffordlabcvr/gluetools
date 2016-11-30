package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class TypingAnalysis {

	@PojoDocumentField
	public String typeAlignmentName;

	@PojoDocumentField
	public String typeAlignmentDisplayName;

	@PojoDocumentField
	public String summaryCode;

	@PojoDocumentField
	public String closestMemberAlignmentName;

	@PojoDocumentField
	public String closestMemberAlignmentDisplayName;

	@PojoDocumentField
	public String closestMemberSourceName;

	@PojoDocumentField
	public String closestMemberSequenceID;

	@PojoDocumentField
	public Double likeWeightRatio;

	@PojoDocumentField
	public Double distanceToClosestMember;

	
}
