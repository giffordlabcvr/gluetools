package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureSegmentException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
	commandWords={"add","segment"}, 
	docoptUsages={"<refStart> <refEnd>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Add a new segment of the reference sequence", 
	furtherHelp="The segment includes the reference sequence nucleotide at <refStart> (numbered from 1) "+
	"and subsequent nucleotides up to and including <refEnd>. "+
	"The new segment's endpoints must satisfy 1 <= refStart <= refEnd <= refSeqLength. "+
	"The new segment must not overlap any existing segment in the feature.") 
public class AddFeatureSegmentCommand extends FeatureLocModeCommand<CreateResult> {

	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	
	private int refStart;
	private int refEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, true);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		if(refStart > refEnd) {
			throw new FeatureSegmentException(Code.FEATURE_SEGMENT_ENDPOINTS_REVERSED, 
					getRefSeqName(), getFeatureName(), Integer.toString(refStart), Integer.toString(refEnd));
		}
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext.getObjectContext(), FeatureLocation.class, 
				FeatureLocation.pkMap(getRefSeqName(), getFeatureName()));
		Sequence refSequence = featureLoc.getReferenceSequence().getSequence();
		int refSeqLength = refSequence.getSequenceObject().getNucleotides().length();
		if(refStart < 1 || refEnd > refSeqLength) {
			throw new FeatureSegmentException(Code.FEATURE_SEGMENT_OUT_OF_RANGE, 
					getRefSeqName(), getFeatureName(), 
					Integer.toString(refSeqLength), Integer.toString(refStart), Integer.toString(refEnd));
		}
		List<FeatureSegment> existingSegments = featureLoc.getSegments();
		existingSegments.forEach(sgmt -> {
			Integer existingRefSeqStart = sgmt.getRefStart();
			Integer existingRefSeqEnd = sgmt.getRefEnd();
			if( (refStart >= existingRefSeqStart && refStart <= existingRefSeqEnd) ||
				(refEnd >= existingRefSeqStart && refEnd <= existingRefSeqEnd) ||
				(refStart <= existingRefSeqStart && refEnd >= existingRefSeqEnd)
			) {
				throw new FeatureSegmentException(Code.FEATURE_SEGMENT_OVERLAPS_EXISTING, 
						getRefSeqName(), getFeatureName(), Integer.toString(refStart), 
						Integer.toString(refEnd), Integer.toString(existingRefSeqStart), Integer.toString(existingRefSeqEnd));
			}
		});
		FeatureSegment featureSegment = GlueDataObject.create(objContext, FeatureSegment.class, 
				FeatureSegment.pkMap(getRefSeqName(), getFeatureName(), refStart, refEnd), false);
		featureSegment.setFeatureLocation(featureLoc);
		cmdContext.commit();
		return new CreateResult(FeatureSegment.class, 1);
	}

}