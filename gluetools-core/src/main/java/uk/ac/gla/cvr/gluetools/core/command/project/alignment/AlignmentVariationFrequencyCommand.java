package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberVariationScanCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.VariationScanMemberCount;

@CommandClass(
		commandWords={"variation", "frequency"}, 
		description = "Compute variation frequencies within the alignment", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> [-m] -f <featureName> [-d] [-v <vWhereClause>]" },
		docoptOptions = { 
		"-c, --recursive                                   Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>     Qualify members",
		"-v <vWhereClause>, --vWhereClause <vWhereClause>  Qualify variations",
		"-r <acRefName>, --acRefName <acRefName>           Ancestor-constraining ref",
		"-m, --multiReference                              Scan across references",
		"-f <featureName>, --featureName <featureName>     Feature containing variations",
		"-d, --descendentFeatures                          Include descendent features",
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this alignment. "+
		"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
		"path between this alignment's reference and the ancestor-constraining reference, in the alignment tree. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference. "+
		"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. ",
		metaTags = {}	
)
public class AlignmentVariationFrequencyCommand extends AlignmentModeCommand<AlignmentVariationFrequencyResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String VARIATION_WHERE_CLAUSE = "vWhereClause";
	public static final String AC_REF_NAME = "acRefName";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String FEATURE_NAME = "featureName";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Optional<Expression> vWhereClause;

	private String acRefName;
	private String featureName;
	private Boolean descendentFeatures;
	private Boolean multiReference;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.vWhereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, VARIATION_WHERE_CLAUSE, false));
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
	}
	
	@Override
	public AlignmentVariationFrequencyResult execute(CommandContext cmdContext) {
		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		
		Alignment alignment = lookupAlignment(cmdContext);

		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, true, whereClause);

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
		

		
		List<VariationScanMemberCount> variationScanMemberCounts = new ArrayList<VariationScanMemberCount>();
		for(ReferenceSequence refToScan : refsToScan) {

			for(Feature featureToScan: featuresToScan) {

				FeatureLocation featureLoc = 
						GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
								FeatureLocation.pkMap(refToScan.getName(), featureToScan.getName()), true);
				if(featureLoc == null) {
					continue; // reference did not have that feature.
				}

				List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, vWhereClause.orElse(null));

				Map<String, VariationInfo> variationNameToInfo = new LinkedHashMap<String, VariationInfo>();
				for(Variation variation: variationsToScan) {
					variationNameToInfo.put(variation.getName(), new VariationInfo(variation));
				}

				for(AlignmentMember almtMember: almtMembers) {
					List<VariationScanResult> variationScanResults = MemberVariationScanCommand.memberVariationScan(cmdContext, almtMember, refToScan, featureLoc, variationsToScan);
					for(VariationScanResult variationScanResult: variationScanResults) {
						VariationInfo variationInfo = variationNameToInfo.get(variationScanResult.getVariation().getName());
						if(variationScanResult.isPresent()) {
							variationInfo.membersConfirmedPresent++;
						} else {
							variationInfo.membersConfirmedAbsent++;
						}
					}
				}

				variationScanMemberCounts.addAll(
						variationNameToInfo.values().stream()
						.map(vInfo -> {
							int membersWherePresent = vInfo.membersConfirmedPresent;
							int membersWhereAbsent = vInfo.membersConfirmedAbsent;
							double pctWherePresent = 100.0 * membersWherePresent / (membersWherePresent + membersWhereAbsent);
							double pctWhereAbsent = 100.0 * membersWhereAbsent / (membersWherePresent + membersWhereAbsent);
							return new VariationScanMemberCount(vInfo.variation, 
									membersWherePresent, pctWherePresent, 
									membersWhereAbsent, pctWhereAbsent);
						})
						.collect(Collectors.toList()));
			}
		}
		
		VariationScanMemberCount.sortVariationScanMemberCounts(variationScanMemberCounts);
		return new AlignmentVariationFrequencyResult(variationScanMemberCounts);
	}

	
	private class VariationInfo {
		Variation variation;
		int membersConfirmedPresent = 0;
		int membersConfirmedAbsent = 0;
		public VariationInfo(Variation variation) {
			super();
			this.variation = variation;
		}
	}

	
	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
