package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import gnu.trove.map.TCharIntMap;
import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.procedure.TCharIntProcedure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcidFrequency;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
		commandWords={"amino-acid", "frequency"}, 
		description = "Compute amino acid frequencies for a given feature location", 
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
public class AlignmentAminoAcidFrequencyCommand extends AlignmentModeCommand<AlignmentAminoAcidFrequencyResult> {

	
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
	public AlignmentAminoAcidFrequencyResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		alignment.getConstrainingRef(); // check constrained
		// check it is a coding feature.
		GlueDataObject
			.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false)
			.getFeature().checkCodesAminoAcids();
		List<LabeledAminoAcidFrequency> resultRowData = alignmentAminoAcidFrequencies(
				cmdContext, getAlignmentName(), acRefName, featureName, whereClause, recursive);
		return new AlignmentAminoAcidFrequencyResult(resultRowData);
	}

	public static List<LabeledAminoAcidFrequency> alignmentAminoAcidFrequencies(
			CommandContext cmdContext, String almtName, String acRefName, String featureName, 
			Optional<Expression> whereClause, Boolean recursive) {
		
		int totalMembers = AlignmentListMemberCommand.countMembers(cmdContext, almtName, recursive, whereClause);
		GlueLogger.getGlueLogger().finest("Computing amino acid frequencies for "+totalMembers+" alignment members");
		
		Map<String, RefCodonInfo> codonToRefCodonInfo = initCodonToRefInfoMap(cmdContext, acRefName, featureName);
		
		int batchSize = 500;
		int offset = 0;
		
		while(offset < totalMembers) {
			int lastBatchIndex = Math.min(offset+batchSize, totalMembers);

			Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
			ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
			FeatureLocation scannedFeatureLoc = 
					GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);

			GlueLogger.getGlueLogger().finest("Retrieving members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);
			List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause, offset, batchSize, batchSize);
			GlueLogger.getGlueLogger().finest("Computing amino acid frequencies for members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);

			for(AlignmentMember almtMember: almtMembers) {
				List<LabeledQueryAminoAcid> labeledQueryAminoAcids = 
						MemberAminoAcidCommand.memberAminoAcids(cmdContext, almtMember, 
								ancConstrainingRef, scannedFeatureLoc);
				for(LabeledQueryAminoAcid labeledQueryAminoAcid: labeledQueryAminoAcids) {
					String codonLabel = labeledQueryAminoAcid.getLabeledAminoAcid().getLabeledCodon().getCodonLabel();
					String aa = labeledQueryAminoAcid.getLabeledAminoAcid().getAminoAcid();
					char aaChar = aa.charAt(0);
					if(aaChar != 'X') { // an X doesn't count as a member covering the codon.
						codonToRefCodonInfo.get(codonLabel).addAaMamber(aaChar);
					}
				}
			}
			cmdContext.newObjectContext();
			offset = offset+batchSize;
		}
		GlueLogger.getGlueLogger().finest("Computed amino acid frequencies for "+totalMembers+" members");
		cmdContext.newObjectContext();

		List<LabeledAminoAcidFrequency> resultRowData = new ArrayList<LabeledAminoAcidFrequency>();
		codonToRefCodonInfo.forEach((codonLabel, refCodonInfo) -> {
				refCodonInfo.aaToMemberCount.forEachEntry(new TCharIntProcedure() {
					@Override
					public boolean execute(char aa, int numMembers) {
						LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(refCodonInfo.labeledCodon, new String(new char[]{aa}));
						double pctMembers = 100.0 * numMembers / (double) refCodonInfo.membersAtCodon;
						LabeledAminoAcidFrequency labeledAminoAcidFrequency = 
								new LabeledAminoAcidFrequency(labeledAminoAcid, numMembers, refCodonInfo.membersAtCodon, pctMembers);
						resultRowData.add(labeledAminoAcidFrequency);
						return true;
					}
				});
		});
		return resultRowData;
	}

	private static Map<String, RefCodonInfo> initCodonToRefInfoMap(
			CommandContext cmdContext, String acRefName, String featureName) {
		Map<String, RefCodonInfo> codonToRefCodonInfo = new LinkedHashMap<String,RefCodonInfo>();
		FeatureLocation scannedFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);
		List<LabeledCodon> labeledCodons = scannedFeatureLoc.getLabeledCodons(cmdContext);
		for(LabeledCodon labeledCodon: labeledCodons) {
			codonToRefCodonInfo.put(labeledCodon.getCodonLabel(), new RefCodonInfo(labeledCodon));
		}
		return codonToRefCodonInfo;
	}


	
	private static class RefCodonInfo {
		LabeledCodon labeledCodon;
		TCharIntMap aaToMemberCount = new TCharIntHashMap();
		int membersAtCodon = 0;

		public RefCodonInfo(LabeledCodon labeledCodon) {
			super();
			this.labeledCodon = labeledCodon;
		}

		public void addAaMamber(char aaChar) {
			aaToMemberCount.adjustOrPutValue(aaChar, 1, 1);
			membersAtCodon++;
		}
	}

	
	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
