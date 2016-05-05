package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a member sequence for variations", 
		docoptUsages = { "-r <acRefName> [-m] -f <featureName> [-d] [-w <whereClause>] [-e]" },
		docoptOptions = { 
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-m, --multiReference                           Scan across references",
		"-f <featureName>, --featureName <featureName>  Feature to scan",
		"-d, --descendentFeatures                       Include descendent features",
		"-w <whereClause>, --whereClause <whereClause>  Qualify variations",
		"-e, --excludeAbsent                            Exclude absent variations",
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this member's alignment. "+
		"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
		"path between the containing alignment's reference and the ancestor-constraining reference, in the alignment tree. "+
		"The <featureName> argument names a feature location which is defined on this reference. "+
		"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. "+
		"The result will be confined to this feature location. "+
		"The <whereClause>, if present, qualifies the set of variations scanned for. "+
		"If --excludeAbsent is used, variations which were confirmed to be absent will not appear in the results.",
		metaTags = {}	
)
public class MemberVariationScanCommand extends MemberModeCommand<MemberVariationScanResult> {

	public static final String AC_REF_NAME = "acRefName";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String FEATURE_NAME = "featureName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	public static final String EXCLUDE_ABSENT = "excludeAbsent";



	private String acRefName;
	private String featureName;
	private Boolean descendentFeatures;
	private Expression whereClause;
	private Boolean multiReference;
	private Boolean excludeAbsent;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
		this.excludeAbsent = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ABSENT, false)).orElse(false);
	}

	@Override
	public MemberVariationScanResult execute(CommandContext cmdContext) {
		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));

		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();

		List<ReferenceSequence> refsToScan;
		if(multiReference) {
			refsToScan = alignment.getAncestorPathReferences(cmdContext, acRefName);
		} else {
			refsToScan = Arrays.asList(alignment.getAncConstrainingRef(cmdContext, acRefName));
		}
		
		List<Feature> featuresToScan = new ArrayList<Feature>();
		featuresToScan.add(namedFeature);
		if(descendentFeatures) {
			featuresToScan.addAll(namedFeature.getDescendents());
		}

		List<VariationScanResult> scanResults = new ArrayList<VariationScanResult>();
		for(ReferenceSequence refToScan: refsToScan) {
			
			for(Feature featureToScan: featuresToScan) {
				FeatureLocation featureLoc = 
						GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
								FeatureLocation.pkMap(refToScan.getName(), featureToScan.getName()), true);
				if(featureLoc == null) {
					continue;
				}
				List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
				if(variationsToScan == null) {
					continue;
				}
				scanResults.addAll(memberVariationScan(cmdContext, almtMember, refToScan, featureLoc, variationsToScan, excludeAbsent));
			}
		}
		VariationScanResult.sortVariationScanResults(scanResults);
		return new MemberVariationScanResult(scanResults);
	}

	public static List<VariationScanResult> memberVariationScan(CommandContext cmdContext,
			AlignmentMember almtMember, ReferenceSequence ancConstrainingRef, FeatureLocation featureLoc,
			List<Variation> variationsToScan, boolean excludeAbsent) {
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
				variationScan(cmdContext, memberToFeatureLocRefNtSegs, variationsToScan, excludeAbsent);
		VariationScanResult.sortVariationScanResults(variationScanResults);

		return variationScanResults;
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
