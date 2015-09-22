package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.List;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ShowReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.ListAlignedSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowFeatureTreeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowSequenceCommand.ReferenceShowSequenceResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class AlignmentResult {
	private String referenceName;
	private String alignmentName;
	private int referenceLength;
	private ReferenceFeatureTreeResult referenceFeatureTreeResult;
	private AbstractSequenceObject referenceSequenceObject;
	private List<QueryAlignedSegment> refToParentAlignedSegments;
	
	public AlignmentResult(String alignmentName) {
		super();
		this.alignmentName = alignmentName;
	}

	public void init(CommandContext cmdContext, String parentAlignmentName) {
		ReferenceFeatureTreeResult featureTreeResult;
		String refSourceName;
		String refSequenceID;
		
		// go into alignment and find reference sequence name
		try (ModeCloser almtMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			ShowReferenceSequenceCommand.ShowReferenceResult showReferenceResult = 
					cmdContext.cmdBuilder(ShowReferenceSequenceCommand.class).execute();
			this.referenceName = showReferenceResult.getReferenceName();
		}

		try(ModeCloser refSeqMode = cmdContext.pushCommandMode("reference", referenceName)) {
			featureTreeResult = cmdContext.cmdBuilder(ReferenceShowFeatureTreeCommand.class)
					.execute();
			ReferenceShowSequenceResult refShowSeqResult = cmdContext.cmdBuilder(ReferenceShowSequenceCommand.class).execute();
			refSourceName = refShowSeqResult.getSourceName();
			refSequenceID = refShowSeqResult.getSequenceID();
		}
		
		if(parentAlignmentName != null) {
			try(ModeCloser almtMode = cmdContext.pushCommandMode("alignment", parentAlignmentName)) {
				try(ModeCloser memberMode = cmdContext.pushCommandMode("member", refSourceName, refSequenceID)) {
					refToParentAlignedSegments = 
							cmdContext.cmdBuilder(ListAlignedSegmentCommand.class).execute().asQueryAlignedSegments();
				}
			}
		}
		referenceSequenceObject = GlueDataObject.lookup(cmdContext.getObjectContext(), Sequence.class, 
				Sequence.pkMap(refSourceName, refSequenceID)).getSequenceObject();
		referenceLength = referenceSequenceObject.getNucleotides(cmdContext).length();
		this.referenceFeatureTreeResult = featureTreeResult;
	}

	public void toDocument(ObjectBuilder objBuilder) {
		objBuilder.setString("alignmentName", alignmentName);
		objBuilder.setString("referenceName", referenceName);
		objBuilder.setInt("referenceLength", referenceLength);
		if(refToParentAlignedSegments != null) {
			ArrayBuilder refToParentAlignedSegmentsArray = objBuilder.setArray("refToParentAlignedSegment");
			for(QueryAlignedSegment refToParentAlignedSegment: refToParentAlignedSegments) {
				refToParentAlignedSegment.toDocument(refToParentAlignedSegmentsArray.addObject());
			}
		}
		ObjectBuilder featureTreeObj = objBuilder.setObject("featureTreeResult");
		Document featureTreeDoc = referenceFeatureTreeResult.getDocument();
		featureTreeObj.setImportedDocument(featureTreeDoc);
	}

	public ReferenceFeatureTreeResult getReferenceFeatureTreeResult() {
		return referenceFeatureTreeResult;
	}

	public String getReferenceName() {
		return referenceName;
	}

	public int getReferenceLength() {
		return referenceLength;
	}

	public List<QueryAlignedSegment> getRefToParentAlignedSegments() {
		return refToParentAlignedSegments;
	}
	
}