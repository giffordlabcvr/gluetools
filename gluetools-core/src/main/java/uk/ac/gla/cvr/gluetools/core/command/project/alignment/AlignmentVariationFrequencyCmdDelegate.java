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
	
	// returns alignment name to list of VariationScanMemberCount
	public Map<String, List<VariationScanMemberCount>> execute(String namedAlignmentName, boolean alignmentRecursive, CommandContext cmdContext) {

		int totalMembers = countTotalMembers(cmdContext, namedAlignmentName);

		Map<String, Map<Map<String,String>, VariationInfo>> almtNameToVarPkMapToInfo = 
				new LinkedHashMap<String, Map<Map<String,String>,VariationInfo>>();

		int offset = 0;
		while(offset < totalMembers) {
			Alignment namedAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(namedAlignmentName));
			int lastBatchIndex = Math.min(offset+BATCH_SIZE+1, totalMembers);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Retrieving members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);
			List<AlignmentMember> memberBatch = AlignmentListMemberCommand
					.listMembers(cmdContext, namedAlignment, recursive, whereClause, offset, BATCH_SIZE, BATCH_SIZE);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Processing members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);

			processBatch(cmdContext, namedAlignment, alignmentRecursive, almtNameToVarPkMapToInfo, memberBatch);
			offset = offset+BATCH_SIZE;
			cmdContext.commit();
			cmdContext.newObjectContext();
		}
		GlueLogger.getGlueLogger().log(Level.FINEST, "Processed "+totalMembers+" members");

		Map<String, List<VariationScanMemberCount>> almtNameToScanCountList = 
				new LinkedHashMap<String, List<VariationScanMemberCount>>();
		almtNameToVarPkMapToInfo.forEach((almtName, varPkMapToInfo) -> {
			List<VariationScanMemberCount> scanCountList = 
					varPkMapToInfo.values().stream()
					.map(vInfo -> {
						int membersWherePresent = vInfo.membersConfirmedPresent;
						int membersWhereAbsent = vInfo.membersConfirmedAbsent;
						double pctWherePresent = 100.0 * membersWherePresent / (membersWherePresent + membersWhereAbsent);
						double pctWhereAbsent = 100.0 * membersWhereAbsent / (membersWherePresent + membersWhereAbsent);
						return new VariationScanMemberCount(vInfo.variationPkMap,
								vInfo.minLocStart,
								membersWherePresent, pctWherePresent, 
								membersWhereAbsent, pctWhereAbsent);
					})
					.collect(Collectors.toList());

			VariationScanMemberCount.sortVariationScanMemberCounts(scanCountList);
			almtNameToScanCountList.put(almtName, scanCountList);
		});
		return almtNameToScanCountList;
	}

	private int countTotalMembers(CommandContext cmdContext,
			String namedAlignmentName) {
		Alignment namedAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(namedAlignmentName));
		GlueLogger.getGlueLogger().log(Level.FINEST, "Searching for alignment members");
		int totalMembers = AlignmentListMemberCommand.countMembers(cmdContext, namedAlignment, recursive, whereClause);
		GlueLogger.getGlueLogger().log(Level.FINEST, "Found "+totalMembers+" alignment members");
		return totalMembers;
	}

	private void processBatch(
			CommandContext cmdContext,
			Alignment namedAlignment,
			boolean alignmentRecursive,
			Map<String, Map<Map<String,String>, VariationInfo>> almtNameToVarPkMapToInfo,
			List<AlignmentMember> memberBatch) {
		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));

		List<ReferenceSequence> refsToScan;
		if(multiReference) {
			refsToScan = namedAlignment.getAncestorPathReferences(cmdContext, acRefName);
		} else {
			refsToScan = Arrays.asList(namedAlignment.getAncConstrainingRef(cmdContext, acRefName));
		}

		List<Feature> featuresToScan = new ArrayList<Feature>();
		featuresToScan.add(namedFeature);
		if(descendentFeatures) {
			featuresToScan.addAll(namedFeature.getDescendents());
		}

		for(ReferenceSequence refToScan : refsToScan) {
			GlueLogger.getGlueLogger().log(Level.FINEST, "Scanning for variations defined on Reference"+refToScan.pkMap());

			for(Feature featureToScan: featuresToScan) {

				FeatureLocation featureLoc = 
						GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
								FeatureLocation.pkMap(refToScan.getName(), featureToScan.getName()), true);
				if(featureLoc == null) {
					continue; // reference did not have that feature.
				}

				List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, vWhereClause.orElse(null));
				if(variationsToScan.isEmpty()) {
					continue;
				}
				GlueLogger.getGlueLogger().log(Level.FINEST, "Scanning for "+variationsToScan.size()+" variations defined on FeatureLoc"+featureLoc.pkMap());

				
				for(AlignmentMember almtMember: memberBatch) {
					List<VariationScanResult> variationScanResults = MemberVariationScanCommand.memberVariationScan(cmdContext, almtMember, refToScan, featureLoc, variationsToScan, false);
					for(VariationScanResult variationScanResult: variationScanResults) {
						registerScanResult(almtNameToVarPkMapToInfo, namedAlignment, alignmentRecursive, almtMember, variationScanResult);
					}
				}

			}
		}
	}

	
	private void registerScanResult(Map<String, Map<Map<String,String>, VariationInfo>> almtNameToVarPkMapToInfo,
			Alignment namedAlignment, boolean alignmentRecursive,
			AlignmentMember almtMember, VariationScanResult variationScanResult) {
		
		List<Alignment> alignmentsToRecord;
		if(alignmentRecursive) {
			alignmentsToRecord = almtMember.getAlignment().getAncestorsUpTo(namedAlignment);
		} else {
			alignmentsToRecord = Arrays.asList(namedAlignment);
		}
		for(Alignment almtToRecord: alignmentsToRecord) {
			Map<Map<String,String>, VariationInfo> varPkMapToInfo = almtNameToVarPkMapToInfo.get(almtToRecord.getName());
			if(varPkMapToInfo == null) {
				varPkMapToInfo	= new LinkedHashMap<Map<String,String>, VariationInfo>();
				almtNameToVarPkMapToInfo.put(almtToRecord.getName(), varPkMapToInfo);
			}
			Variation variation = variationScanResult.getVariation();
			Map<String,String> varPkMap = variation.pkMap();
			VariationInfo variationInfo = varPkMapToInfo.get(varPkMap);
			if(variationInfo == null) {
				variationInfo = new VariationInfo(varPkMap, variation.minLocStart());
				varPkMapToInfo.put(varPkMap, variationInfo);
			}
			if(variationScanResult.isPresent()) {
				variationInfo.membersConfirmedPresent++;
			} else {
				variationInfo.membersConfirmedAbsent++;
			}
		}
	}


	private class VariationInfo {
		Map<String,String> variationPkMap;
		int minLocStart;
		int membersConfirmedPresent = 0;
		int membersConfirmedAbsent = 0;
		public VariationInfo(Map<String,String> variationPkMap, int minLocStart) {
			super();
			this.variationPkMap = variationPkMap;
			this.minLocStart = minLocStart;
		}
	}

	
}
