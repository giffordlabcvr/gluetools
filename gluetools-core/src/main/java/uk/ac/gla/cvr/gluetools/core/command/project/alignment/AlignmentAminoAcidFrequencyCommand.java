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

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcidFrequency;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAminoAcidColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
		commandWords={"amino-acid", "frequency"}, 
		description = "Compute amino acid frequencies for a given feature location", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> -f <featureName> [-l <lcStart> <lcEnd>]" },
		docoptOptions = { 
		"-c, --recursive                                Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>  Feature to translate",
		"-l, --labelledCodon                            Region between codon labels",
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
	public static final String LABELLED_CODON = "labelledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";


	// could possibly use FastaAlignmentExportCommandDelegate here.
	
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private String acRefName;
	private String featureName;
	private boolean labelledCodon;
	private String lcStart;
	private String lcEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.labelledCodon = PluginUtils.configureBooleanProperty(configElem, LABELLED_CODON, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		this.lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		if(labelledCodon && (lcStart == null || lcEnd == null)) {
			usageErrorLC();
		}

	}
	
	private void usageErrorLC() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --labelledCodon is used, both <lcStart> and <lcEnd> must be specified");
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
				cmdContext, getAlignmentName(), acRefName, featureName, whereClause, recursive, lcStart, lcEnd);
		return new AlignmentAminoAcidFrequencyResult(resultRowData);
	}

	public static List<LabeledAminoAcidFrequency> alignmentAminoAcidFrequencies(
			CommandContext cmdContext, String almtName, String relRefName, String featureName, 
			Optional<Expression> whereClause, Boolean recursive, 
			String lcStart, String lcEnd) {

		SimpleAminoAcidColumnsSelector almtColsSelector = 
					new SimpleAminoAcidColumnsSelector(relRefName, featureName, lcStart, lcEnd);
		
		LinkedHashMap<String, RefCodonInfo> codonToRefCodonInfo = 
				initCodonToRefInfoMap(cmdContext, relRefName, featureName, lcStart, lcEnd);
		
		String[] codonLabels = codonToRefCodonInfo.keySet().toArray(new String[]{});
		
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(almtName, recursive, whereClause);

		AbstractAlmtRowConsumer almtRowConsumer = new AbstractAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString) {
				for(int i = 0; i < alignmentRowString.length(); i++) {
					char aaChar = alignmentRowString.charAt(i);
					String codonLabel = codonLabels[i];
					if(aaChar != 'X' && aaChar != '-') { // an X or '-' doesn't count as a member covering the codon.
						codonToRefCodonInfo.get(codonLabel).addAaMamber(aaChar);
					}
				}
			}
		};
		almtColsSelector.generateAlignmentRows(cmdContext,
				true, queryMemberSupplier, almtRowConsumer);
		
		return formLabeledAminoAcidFrequencies(codonToRefCodonInfo);
	}
	
	/*
	 * old implementation which did not use FastaProteinAlignmentExporter
	public static List<LabeledAminoAcidFrequency> alignmentAminoAcidFrequenciesOld(
			CommandContext cmdContext, String almtName, String acRefName, String featureName, 
			Optional<Expression> whereClause, Boolean recursive) {
		
		int totalMembers = AlignmentListMemberCommand.countMembers(cmdContext, almtName, recursive, whereClause);
		GlueLogger.getGlueLogger().finest("Computing amino acid frequencies for "+totalMembers+" alignment members");
		
		Map<String, RefCodonInfo> codonToRefCodonInfo = initCodonToRefInfoMap(cmdContext, acRefName, featureName, null, null);
		
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

		return formLabeledAminoAcidFrequencies(codonToRefCodonInfo);
	}
	*/

	private static List<LabeledAminoAcidFrequency> formLabeledAminoAcidFrequencies(
			Map<String, RefCodonInfo> codonToRefCodonInfo) {
		List<LabeledAminoAcidFrequency> resultRowData = new ArrayList<LabeledAminoAcidFrequency>();
		codonToRefCodonInfo.forEach((codonLabel, refCodonInfo) -> {
				refCodonInfo.aaToMemberCount.forEachEntry(new TCharIntProcedure() {
					@Override
					public boolean execute(char aa, int numMembers) {
						double pctMembers = 100.0 * numMembers / (double) refCodonInfo.membersAtCodon;
						String aminoAcid = new String(new char[]{aa});
						LabeledAminoAcidFrequency labeledAminoAcidFrequency = 
								new LabeledAminoAcidFrequency(refCodonInfo.labeledCodon, aminoAcid, numMembers, refCodonInfo.membersAtCodon, pctMembers);
						resultRowData.add(labeledAminoAcidFrequency);
						return true;
					}
				});
		});
		return resultRowData;
	}

	private static LinkedHashMap<String, RefCodonInfo> initCodonToRefInfoMap(
			CommandContext cmdContext, String acRefName, String featureName, String lcStartString, String lcEndString) {
		LinkedHashMap<String, RefCodonInfo> codonToRefCodonInfo = new LinkedHashMap<String,RefCodonInfo>();
		FeatureLocation scannedFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);
		List<LabeledCodon> labeledCodons = scannedFeatureLoc.getLabeledCodons(cmdContext);
		LabeledCodon lcStart = null;
		if(lcStartString != null) {
			lcStart = scannedFeatureLoc.getLabeledCodon(cmdContext, lcStartString);
		}
		LabeledCodon lcEnd = null;
		if(lcEndString != null) {
			lcEnd = scannedFeatureLoc.getLabeledCodon(cmdContext, lcEndString);
		}
		for(LabeledCodon labeledCodon: labeledCodons) {
			if(lcStart != null && labeledCodon.getNtStart() < lcStart.getNtStart()) {
				continue;
			}
			if(lcEnd != null && labeledCodon.getNtStart() > lcEnd.getNtStart()) {
				continue;
			}
			RefCodonInfo refCodonInfo = new RefCodonInfo(labeledCodon);
			codonToRefCodonInfo.put(labeledCodon.getCodonLabel(), refCodonInfo);
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
