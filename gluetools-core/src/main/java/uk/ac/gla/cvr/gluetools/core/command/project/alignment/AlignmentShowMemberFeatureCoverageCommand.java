package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"show", "member", "feature-coverage"}, 
		description = "Show coverage of a given feature location by specific members", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> -f <featureName>" },
		docoptOptions = { 
		"-c, --recursive                                Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>  Feature to translate"
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this alignment. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference.",
				metaTags = {}	
)
public class AlignmentShowMemberFeatureCoverageCommand extends AlignmentModeCommand<AlignmentShowMemberFeatureCoverageResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	
	private Boolean recursive;
	private Optional<Expression> whereClause;

	private String acRefName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
	}
	
	@Override
	public AlignmentShowMemberFeatureCoverageResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
		FeatureLocation scannedFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);
		List<MemberFeatureCoverage> resultRowData = alignmentFeatureCoverage(
				cmdContext, alignment, ancConstrainingRef, scannedFeatureLoc, almtMembers);
		return new AlignmentShowMemberFeatureCoverageResult(resultRowData);
	}

	public static List<MemberFeatureCoverage> alignmentFeatureCoverage(
			CommandContext cmdContext, Alignment alignment, 
			ReferenceSequence ancConstrainingRef, FeatureLocation scannedFeatureLoc,
			List<AlignmentMember> almtMembers) {

		List<MemberFeatureCoverage> membFeatCvrgList = new ArrayList<MemberFeatureCoverage>();
			
		Integer featureLength = IReferenceSegment.totalReferenceLength(scannedFeatureLoc.segmentsAsReferenceSegments());
		
		for(AlignmentMember almtMember: almtMembers) {
			
			Alignment tipAlmt = almtMember.getAlignment();
			
			List<QueryAlignedSegment> memberToConstrainingRefSegs = almtMember.segmentsAsQueryAlignedSegments();
			List<QueryAlignedSegment> memberToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, memberToConstrainingRefSegs, ancConstrainingRef);

			// trim down to the feature area.
			List<ReferenceSegment> featureLocRefSegs = scannedFeatureLoc.segmentsAsReferenceSegments();
			
			List<QueryAlignedSegment> memberToFeatureLocRefSegs = ReferenceSegment.intersection(memberToAncConstrRefSegsFull, featureLocRefSegs,
					ReferenceSegment.cloneLeftSegMerger());

			Double refNtCvrgPct = IQueryAlignedSegment.getReferenceNtCoveragePercent(memberToFeatureLocRefSegs, featureLength);
			membFeatCvrgList.add(new MemberFeatureCoverage(almtMember, refNtCvrgPct));
		}
		return membFeatCvrgList;
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
