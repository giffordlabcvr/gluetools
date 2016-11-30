package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class QueryNtSegment extends NtSegment {

	@PojoDocumentListField(itemClass = ReferenceDiffNtMask.class)
	public List<ReferenceDiffNtMask> referenceDiffs = new ArrayList<ReferenceDiffNtMask>();

}
