package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"show", "feature-presence"}, 
		description = "Show presence of features across a set of members", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> [-p <presencePct>]" },
		docoptOptions = { 
		"-c, --recursive                                Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-p <presencePct>, --presencePct <presencePct>  Percentage coverage for presence"
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this alignment. "+
		"Selected members will be scanned for presence of all features located on the named reference sequence. "+
		"Presence is defined as having nucleotide reference coverage percentage higher than <presencePct>, which defaults to "+
		"90.0",
		metaTags = {}	
)
public class AlignmentShowFeaturePresenceCommand extends AlignmentModeCommand<AlignmentShowFeaturePresenceResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String AC_REF_NAME = "acRefName";
	public static final String PRESENCE_PCT = "presencePct";
	
	private static final int BATCH_SIZE = 500;
	
	private Boolean recursive;
	private Optional<Expression> whereClause;

	private String acRefName;
	private Double presencePct;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.presencePct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, PRESENCE_PCT, 0.0, true, 100.0, true, false)).orElse(90.0);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
	}
	
	@Override
	public AlignmentShowFeaturePresenceResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		GlueLogger.getGlueLogger().finest("Searching for members to process");
		SelectQuery selectQuery = new SelectQuery(AlignmentMember.class, AlignmentListMemberCommand.getMatchExpression(alignment, recursive, whereClause));
		int totalMembers = GlueDataObject.count(cmdContext, selectQuery);
		GlueLogger.getGlueLogger().finest("Found "+totalMembers+" members to process");
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
		List<FeatureLocation> featureLocations = ancConstrainingRef.getFeatureLocations();
		
		Map<Map<String,String>,FeaturePresence> fLocPkMapToMembFeatCvg = new LinkedHashMap<Map<String,String>,FeaturePresence>();

		for(FeatureLocation scannedFeatureLoc: featureLocations) {
			fLocPkMapToMembFeatCvg.put(scannedFeatureLoc.pkMap(), new FeaturePresence(scannedFeatureLoc, 0, totalMembers));
		}
		
		int offset = 0;
		selectQuery.setFetchLimit(BATCH_SIZE);
		selectQuery.setPageSize(BATCH_SIZE);
		while(offset < totalMembers) {
			selectQuery.setFetchOffset(offset);
			
			int lastBatchIndex = Math.min(offset+BATCH_SIZE, totalMembers);
			GlueLogger.getGlueLogger().finest("Retrieving members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);
			List<AlignmentMember> almtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
			GlueLogger.getGlueLogger().finest("Processing members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);
			for(AlignmentMember almtMember: almtMembers) {
				for(FeatureLocation scannedFeatureLoc: featureLocations) {
					MemberFeatureCoverage membFeatCoverage = alignmentFeatureCoverage(
							cmdContext, ancConstrainingRef, scannedFeatureLoc, almtMember);
					if(membFeatCoverage.getFeatureReferenceNtCoverage() >= presencePct) {
						fLocPkMapToMembFeatCvg.get(scannedFeatureLoc.pkMap()).incrementMembersWherePresent();
					}
				}
			}
			offset += BATCH_SIZE;
		}
		return new AlignmentShowFeaturePresenceResult(new ArrayList<FeaturePresence>(fLocPkMapToMembFeatCvg.values()));
	}

	public static MemberFeatureCoverage alignmentFeatureCoverage(
			CommandContext cmdContext, 
			ReferenceSequence ancConstrainingRef, FeatureLocation scannedFeatureLoc,
			AlignmentMember almtMember) {
		Integer featureLength = IReferenceSegment.totalReferenceLength(scannedFeatureLoc.segmentsAsReferenceSegments());

		Alignment tipAlmt = almtMember.getAlignment();

		List<QueryAlignedSegment> memberToConstrainingRefSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, memberToConstrainingRefSegs, ancConstrainingRef);

		// trim down to the feature area.
		List<ReferenceSegment> featureLocRefSegs = scannedFeatureLoc.segmentsAsReferenceSegments();

		List<QueryAlignedSegment> memberToFeatureLocRefSegs = ReferenceSegment.intersection(memberToAncConstrRefSegsFull, featureLocRefSegs,
				ReferenceSegment.cloneLeftSegMerger());

		Double refNtCvrgPct = IQueryAlignedSegment.getReferenceNtCoveragePercent(memberToFeatureLocRefSegs, featureLength);
		return new MemberFeatureCoverage(almtMember, refNtCvrgPct);
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
