package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberVariationScanCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.VariationScanMemberCount;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

public class AlignmentVariationFrequencyCmdDelegate {

	public static final int BATCH_SIZE = 1000;
	
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
	
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.vWhereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, VARIATION_WHERE_CLAUSE, false));
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
	}
	
	public List<VariationScanMemberCount> execute(Alignment alignment, CommandContext cmdContext) {
		List<VariationScanMemberCount> variationScanMemberCounts = new ArrayList<VariationScanMemberCount>();

		GlueLogger.getGlueLogger().log(Level.FINEST, "Searching for alignment members");
		int totalMembers = AlignmentListMemberCommand.countMembers(cmdContext, alignment, recursive, whereClause);
		GlueLogger.getGlueLogger().log(Level.FINEST, "Found "+totalMembers+" alignment members");

		int offset = 0;
		while(offset < totalMembers) {

			int lastBatchIndex = Math.min(offset+BATCH_SIZE+1, totalMembers);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Retrieving members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);
			List<AlignmentMember> memberBatch = AlignmentListMemberCommand
					.listMembers(cmdContext, alignment, recursive, whereClause, offset, BATCH_SIZE, BATCH_SIZE);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Processing members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);

			Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));

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

					for(AlignmentMember almtMember: memberBatch) {
						List<VariationScanResult> variationScanResults = MemberVariationScanCommand.memberVariationScan(cmdContext, almtMember, refToScan, featureLoc, variationsToScan, false);
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
			offset = offset+BATCH_SIZE;
			cmdContext.commit();
			cmdContext.newObjectContext();
		}
		GlueLogger.getGlueLogger().log(Level.FINEST, "Processed "+totalMembers+" members");

		VariationScanMemberCount.sortVariationScanMemberCounts(variationScanMemberCounts);
		return variationScanMemberCounts;
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

	
}
