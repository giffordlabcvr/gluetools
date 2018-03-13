/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a member sequence for variations", 
		docoptUsages = { "-r <acRefName> [-m] -f <featureName> [-d] [-w <whereClause>] [-e] [-l [-v [-n] [-o]]]" },
		docoptOptions = { 
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-m, --multiReference                           Scan across references",
		"-f <featureName>, --featureName <featureName>  Feature to scan",
		"-d, --descendentFeatures                       Include descendent features",
		"-w <whereClause>, --whereClause <whereClause>  Qualify variations",
		"-e, --excludeAbsent                            Exclude absent variations",
		"-l, --showPatternLocsSeparately                Add row per pattern location",
		"-v, --showMatchValuesSeparately                Add row per match value",
		"-n, --showMatchNtLocations                     Add match NT start/end columns",
		"-o, --showMatchLcLocations                     Add codon start/end columns",
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
	private VariationScanRenderHints variationScanRenderHints = new VariationScanRenderHints();
	
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
		this.variationScanRenderHints.configure(pluginConfigContext, configElem);
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

		List<VariationScanResult<?>> scanResults = new ArrayList<VariationScanResult<?>>();
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
		return new MemberVariationScanResult(variationScanRenderHints, scanResults);
	}

	public static List<VariationScanResult<?>> memberVariationScan(CommandContext cmdContext,
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
		
		
		List<VariationScanResult<?>> variationScanResults = featureLoc.
				variationScan(cmdContext, memberToFeatureLocRefNtSegs, variationsToScan, excludeAbsent);
		VariationScanResult.sortVariationScanResults(variationScanResults);

		return variationScanResults;
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
