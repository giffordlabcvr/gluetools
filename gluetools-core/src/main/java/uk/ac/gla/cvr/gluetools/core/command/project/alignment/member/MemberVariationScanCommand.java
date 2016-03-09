package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a member sequence for variations", 
		docoptUsages = { "-r <acRefName> -f <featureName> [-w <whereClause>]" },
		docoptOptions = { 
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>  Feature to scan",
		"-w <whereClause>, --whereClause <whereClause>  Qualify variations",
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this member's alignment. "+
		"The <featureName> argument names a feature location which is defined on this reference. "+
		"The result will be confined to this feature location. "+
		"The <whereClause>, if present, qualifies the set of variations scanned for.",
		metaTags = {}	
)
public class MemberVariationScanCommand extends MemberModeCommand<MemberVariationScanResult> {

	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String WHERE_CLAUSE = "whereClause";

	private String referenceName;
	private String featureName;
	private Expression whereClause;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.referenceName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
	}

	@Override
	public MemberVariationScanResult execute(CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, referenceName);
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(referenceName, featureName), false);
		return memberVariationScan(cmdContext, almtMember, ancConstrainingRef, featureLoc, whereClause);
	}

	public static MemberVariationScanResult memberVariationScan(CommandContext cmdContext,
			AlignmentMember almtMember, ReferenceSequence ancConstrainingRef, FeatureLocation featureLoc,
			Expression whereClause) {
		Alignment tipAlmt = almtMember.getAlignment();
		
		List<QueryAlignedSegment> memberToConstrainingRefSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, memberToConstrainingRefSegs, ancConstrainingRef);

		// trim down to the feature area.
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		List<QueryAlignedSegment> memberToFeatureLocRefSegs = ReferenceSegment.intersection(memberToAncConstrRefSegsFull, featureLocRefSegs,
				ReferenceSegment.cloneLeftSegMerger());
		
		AbstractSequenceObject memberSeqObj = almtMember.getSequence().getSequenceObject();
		
		List<NtQueryAlignedSegment> memberToFeatureLocRefNtSegs = 
				memberToFeatureLocRefSegs.stream()
				.map(seg -> new NtQueryAlignedSegment(
						seg.getRefStart(), seg.getRefEnd(), 
						seg.getQueryStart(), seg.getQueryEnd(), 
						memberSeqObj.getNucleotides(cmdContext, seg.getQueryStart(), seg.getQueryEnd())))
				.collect(Collectors.toList());
		
		List<VariationScanResult> variationScanResults = featureLoc.
				variationScan(cmdContext, memberToFeatureLocRefNtSegs, whereClause);
		
		return new MemberVariationScanResult(variationScanResults);
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
