package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

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
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> -f <featureName> [-v <vWhereClause>]" },
		docoptOptions = { 
		"-c, --recursive                                   Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>     Qualify members",
		"-v <vWhereClause>, --vWhereClause <vWhereClause>  Qualify variations",
		"-r <acRefName>, --acRefName <acRefName>           Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>     Feature containing variations"
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this alignment. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference.",
				metaTags = {}	
)
public class AlignmentVariationFrequencyCommand extends AlignmentModeCommand<AlignmentVariationFrequencyResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String VARIATION_WHERE_CLAUSE = "vWhereClause";
	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Optional<Expression> vWhereClause;

	private String referenceName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.referenceName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		vWhereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, VARIATION_WHERE_CLAUSE, false));
	}
	
	@Override
	public AlignmentVariationFrequencyResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, true, whereClause);
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, referenceName);
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(referenceName, featureName), false);

		List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, vWhereClause.orElse(null));
		
        Map<String, VariationInfo> variationNameToInfo = new LinkedHashMap<String, VariationInfo>();
		for(Variation variation: variationsToScan) {
			variationNameToInfo.put(variation.getName(), new VariationInfo(variation));
		}
		
		for(AlignmentMember almtMember: almtMembers) {
			List<VariationScanResult> variationScanResults = MemberVariationScanCommand.memberVariationScan(cmdContext, almtMember, ancConstrainingRef, featureLoc, variationsToScan);
			for(VariationScanResult variationScanResult: variationScanResults) {
				VariationInfo variationInfo = variationNameToInfo.get(variationScanResult.getVariation().getName());
				if(variationScanResult.isPresent()) {
					variationInfo.membersConfirmedPresent++;
				} else if(variationScanResult.isAbsent()) {
					variationInfo.membersConfirmedAbsent++;
				}
			}
		}

		List<VariationScanMemberCount> variationScanMemberCounts = 
				variationNameToInfo.values().stream()
				.map(vInfo -> {
					int membersWherePresent = vInfo.membersConfirmedPresent;
					int membersWhereAbsent = vInfo.membersConfirmedAbsent;
					double pctWherePresent = 100.0 * membersWherePresent / (membersWherePresent + membersWhereAbsent);
					double pctWhereAbsent = 100.0 * membersWhereAbsent / (membersWherePresent + membersWhereAbsent);
					return new VariationScanMemberCount(vInfo.variation.getName(), 
							membersWherePresent, pctWherePresent, 
							membersWhereAbsent, pctWhereAbsent);
				})
				.collect(Collectors.toList());

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
