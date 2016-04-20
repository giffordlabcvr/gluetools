package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import gnu.trove.map.TIntCharMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntCharHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.FastaSequenceObject;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceReporter;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceReporter.TranslatedQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.AllColumnsAlignment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="webAnalysisTool")
public class WebAnalysisTool extends ModulePlugin<WebAnalysisTool> {

	public static final String FASTA_SEQUENCE_REPORTER_MODULE_NAME = "fastaSequenceReporterModuleName";
	public static final String FEATURE_ANALYSIS_HINT = "featureAnalysisHint";

	private String fastaSequenceReporterModuleName;
	private List<FeatureAnalysisHint> featureAnalysisHints;
	
	public WebAnalysisTool() {
		super();
		addModulePluginCmdClass(WebAnalysisCommand.class);
		addModulePluginCmdClass(AnalysisCommand.class);
		addSimplePropertyName(FASTA_SEQUENCE_REPORTER_MODULE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fastaSequenceReporterModuleName = PluginUtils.configureStringProperty(configElem, FASTA_SEQUENCE_REPORTER_MODULE_NAME, true);
		featureAnalysisHints = 
				PluginFactory.createPlugins(pluginConfigContext, FeatureAnalysisHint.class, 
						GlueXmlUtils.getXPathElements(configElem, FEATURE_ANALYSIS_HINT));
	}

	public WebAnalysisResult analyse(CommandContext cmdContext, byte[] fastaBytes) {
		
		FastaSequenceReporter fastaSequenceReporter = 
				Module.resolveModulePlugin(cmdContext, FastaSequenceReporter.class, fastaSequenceReporterModuleName);
		
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> fastaIdToSequence = FastaUtils.parseFasta(fastaBytes);
		Map<String, SequenceAnalysis> fastaIdToSequenceAnalysis = new LinkedHashMap<String, SequenceAnalysis>();
		Map<String, ReferenceAnalysis> refNameToAnalysis = new LinkedHashMap<String, ReferenceAnalysis>();
		AllColumnsAlignment<Key> allColsAlmt = null;

		fastaIdToSequence.forEach((fastaId, sequence) -> {
			String targetRefName = fastaSequenceReporter.targetRefNameFromFastaId(cmdContext, fastaId);
			ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));
			AlignmentMember tipAlmtMember = targetRef.getTipAlignmentMembership(null);
			Alignment tipAlmt = tipAlmtMember.getAlignment();
			List<Alignment> ancestors = tipAlmt.getAncestors();
			// reverse order to ensure parent is added before child.
			for(int i = ancestors.size()-1; i >= 0; i--) {
				Alignment ancestor = ancestors.get(i);
				ReferenceSequence ancRefSeq = ancestor.getRefSequence();
				String refName = ancRefSeq.getName();
				if(!refNameToAnalysis.containsKey(refName)) {
					Alignment parentAlmt = ancestor.getParent();
					AlignmentMember parentAlmtMember = null;
					if(parentAlmt != null) {
						parentAlmtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
							AlignmentMember.pkMap(parentAlmt.getName(),
									ancRefSeq.getSequence().getSource().getName(), 
									ancRefSeq.getSequence().getSequenceID()));
					}
					refNameToAnalysis.put(refName, new ReferenceAnalysis(ancRefSeq, parentAlmt, parentAlmtMember));
				}
			}
			if(!targetRefName.equals(tipAlmt.getRefSequence().getName())) {
				if(!refNameToAnalysis.containsKey(targetRefName)) {
					refNameToAnalysis.put(targetRefName, 
						new ReferenceAnalysis(targetRef, tipAlmt, tipAlmtMember));
				}
			}
			fastaIdToSequenceAnalysis.put(fastaId, new SequenceAnalysis(fastaId, new FastaSequenceObject(fastaId, sequence.getSequenceAsString()), targetRefName));
		});
		// Add all reference sequences to the all-column alignment.
		for(Map.Entry<String, ReferenceAnalysis> entry: refNameToAnalysis.entrySet()) {
			String refName = entry.getKey();
			ReferenceAnalysis refAnalysis = entry.getValue();
			int length = 
					refAnalysis.getRefSeq().getSequence().getSequenceObject()
					.getNucleotides(cmdContext).length();
			if(refAnalysis.parentRefName == null) {
				allColsAlmt = new AllColumnsAlignment<Key>(new ReferenceKey(refName), length);
			} else {
				allColsAlmt.addRow(new ReferenceKey(refName), 
						new ReferenceKey(refAnalysis.parentRefName), 
						refAnalysis.getContainingAlmtMember().segmentsAsQueryAlignedSegments(),
						length);
			}
		}
		// Add all the query sequences to the all-column alignment
		for(Map.Entry<String, SequenceAnalysis> entry: fastaIdToSequenceAnalysis.entrySet()) {
			String fastaID = entry.getKey();
			SequenceAnalysis seqAnalysis = entry.getValue();
			DNASequence sequence = fastaIdToSequence.get(fastaID);
			List<QueryAlignedSegment> queryToTargetRefSegs = generateSequenceTargetAlignment(
					cmdContext, fastaSequenceReporter, fastaID, sequence,
					seqAnalysis.targetRefName);
			seqAnalysis.setQueryToTargetRefSegs(queryToTargetRefSegs);
			allColsAlmt.addRow(new QueryKey(fastaID), 
					new ReferenceKey(seqAnalysis.targetRefName), 
					queryToTargetRefSegs,
					sequence.getSequenceAsString().length());
		}
		
		// rationalise the all-column alignment by merging abutting segments.
		allColsAlmt.rationalise();
		
		
		for(ReferenceAnalysis refAnalysis: refNameToAnalysis.values()) {
			refAnalysis.ntAlignedSegment = new ArrayList<NtAlignedSegment>();
			List<QueryAlignedSegment> refToUSegs = allColsAlmt.getSegments(new ReferenceKey(refAnalysis.refName));
			// populate NT aligned segments for reference sequences.
			for(QueryAlignedSegment refToUSeg: refToUSegs) {
				CharSequence segNTs = refAnalysis.getRefSeq().getSequence().getSequenceObject().
					getNucleotides(cmdContext, refToUSeg.getQueryStart(), refToUSeg.getQueryEnd());
				NtAlignedSegment ntAlignedSegment = new NtAlignedSegment();
				ntAlignedSegment.startSeqIndex = refToUSeg.getQueryStart();
				ntAlignedSegment.endSeqIndex = refToUSeg.getQueryEnd();
				ntAlignedSegment.startUIndex = refToUSeg.getRefStart();
				ntAlignedSegment.endUIndex = refToUSeg.getRefEnd();
				ntAlignedSegment.nucleotides = segNTs.toString();
				refAnalysis.ntAlignedSegment.add(ntAlignedSegment);
			}
			// sequence feature analyses
			List<SequenceFeatureAnalysis> sequenceFeatureAnalyses = new ArrayList<SequenceFeatureAnalysis>();
			for(FeatureAnalysisHint featureAnalysisHint: featureAnalysisHints) {
				if(featureAnalysisHint.getIncludeTranslation()) {
					String featureName = featureAnalysisHint.getFeatureName();
					FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
							FeatureLocation.pkMap(refAnalysis.refName, featureName));
					List<QueryAlignedSegment> refToRefSegs = new ArrayList<QueryAlignedSegment>();
					String refNTs = refAnalysis.getRefSeq().getSequence().getSequenceObject().getNucleotides(cmdContext);
					refToRefSegs.add(new QueryAlignedSegment(1, refNTs.length(), 1, refNTs.length()));
					List<TranslatedQueryAlignedSegment> translatedQaSegs = 
							fastaSequenceReporter.translateNucleotides(cmdContext, featureLoc, refToRefSegs, refNTs);
					List<AaSegment> aaSegs = generateAaSegs(translatedQaSegs, refToUSegs);
					SequenceFeatureAnalysis sequenceFeatureAnalysis = new SequenceFeatureAnalysis();
					sequenceFeatureAnalysis.featureName = featureName;
					sequenceFeatureAnalysis.aaSegment = aaSegs;
					sequenceFeatureAnalyses.add(sequenceFeatureAnalysis);
				}
			}
			refAnalysis.sequenceFeatureAnalysis = sequenceFeatureAnalyses;
		}
		
		for(SequenceAnalysis seqAnalysis: fastaIdToSequenceAnalysis.values()) {

			seqAnalysis.ntAlignedSegment = new ArrayList<NtAlignedSegment>();
			List<QueryAlignedSegment> queryToUSegs = allColsAlmt.getSegments(new QueryKey(seqAnalysis.fastaId));
			// populate NT aligned segments for query sequences.
			for(QueryAlignedSegment qaSeg: queryToUSegs) {
				CharSequence segNTs = SegmentUtils.base1SubString(seqAnalysis.getSequenceObj().getNucleotides(cmdContext), 
						qaSeg.getQueryStart(), qaSeg.getQueryEnd());
				NtAlignedSegment ntAlignedSegment = new NtAlignedSegment();
				ntAlignedSegment.startSeqIndex = qaSeg.getQueryStart();
				ntAlignedSegment.endSeqIndex = qaSeg.getQueryEnd();
				ntAlignedSegment.startUIndex = qaSeg.getRefStart();
				ntAlignedSegment.endUIndex = qaSeg.getRefEnd();
				ntAlignedSegment.nucleotides = segNTs.toString();
				seqAnalysis.ntAlignedSegment.add(ntAlignedSegment);
			}
			
			
			// sequence feature analyses
			List<SequenceFeatureAnalysis> sequenceFeatureAnalyses = new ArrayList<SequenceFeatureAnalysis>();
			for(FeatureAnalysisHint featureAnalysisHint: featureAnalysisHints) {
				if(featureAnalysisHint.getIncludeTranslation()) {
					String featureName = featureAnalysisHint.getFeatureName();
					FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
							FeatureLocation.pkMap(seqAnalysis.targetRefName, featureName));
					String queryNTs = seqAnalysis.getSequenceObj().getNucleotides(cmdContext);
					List<QueryAlignedSegment> queryToTargetRefSegs = seqAnalysis.getQueryToTargetRefSegs();
					List<TranslatedQueryAlignedSegment> translatedQaSegs = 
							fastaSequenceReporter.translateNucleotides(cmdContext, featureLoc, queryToTargetRefSegs, queryNTs);
					List<AaSegment> aaSegs = generateAaSegs(translatedQaSegs, queryToUSegs);
					SequenceFeatureAnalysis sequenceFeatureAnalysis = new SequenceFeatureAnalysis();
					sequenceFeatureAnalysis.featureName = featureName;
					sequenceFeatureAnalysis.aaSegment = aaSegs;
					sequenceFeatureAnalyses.add(sequenceFeatureAnalysis);
				}
			}
			seqAnalysis.sequenceFeatureAnalysis = sequenceFeatureAnalyses;
		}

		Map<String, FeatureAnalysis> featureNameToAnalysis = new LinkedHashMap<String, FeatureAnalysis>();
		
		for(FeatureAnalysisHint featureAnalysisHint: featureAnalysisHints) {
			String featureName = featureAnalysisHint.getFeatureName();
			TIntObjectMap<FeatureCodonLabel> uIndexToCodonLabel = new TIntObjectHashMap<FeatureCodonLabel>();
			for(String refName: refNameToAnalysis.keySet()) {
				FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class,
						FeatureLocation.pkMap(refName, featureName), false);
				if(featureLoc != null) {
					List<ReferenceSegment> featureLocReferenceSegments = featureLoc.segmentsAsReferenceSegments();
					List<QueryAlignedSegment> featureLocQaSegments = featureLocReferenceSegments.stream()
							.map(seg -> new QueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getRefStart(), seg.getRefEnd()))
							.collect(Collectors.toList());
					List<QueryAlignedSegment> refToUSegs = allColsAlmt.getSegments(new ReferenceKey(refName));
					List<QueryAlignedSegment> featureLocRefToUSegs = QueryAlignedSegment.translateSegments(featureLocQaSegments, refToUSegs);
					List<LabeledCodon> labeledCodons = featureLoc.getLabeledCodons(cmdContext);
					TIntObjectMap<String> refNtToCodonLabel = new TIntObjectHashMap<String>();
					for(LabeledCodon labeledCodon: labeledCodons) {
						refNtToCodonLabel.put(labeledCodon.getNtStart(), labeledCodon.getCodonLabel());
					}
					for(QueryAlignedSegment qaSeg: featureLocRefToUSegs) {
						int refToUOffset = qaSeg.getQueryToReferenceOffset();
						for(int i = qaSeg.getQueryStart(); i <= qaSeg.getQueryEnd(); i++) {
							String codonLabel = refNtToCodonLabel.get(i);
							if(codonLabel != null) {
								FeatureCodonLabel featureCodonLabel = new FeatureCodonLabel();
								featureCodonLabel.startUIndex = i+refToUOffset;
								featureCodonLabel.endUIndex = i+refToUOffset+2;
								featureCodonLabel.codonLabel = codonLabel;
								uIndexToCodonLabel.put(i+refToUOffset, featureCodonLabel);
							}
						}
					}
				}
			}
			List<FeatureCodonLabel> featureCodonLabels = new ArrayList<FeatureCodonLabel>(uIndexToCodonLabel.valueCollection());
			featureCodonLabels.sort(new Comparator<FeatureCodonLabel>() {
				@Override
				public int compare(FeatureCodonLabel o1, FeatureCodonLabel o2) {
					return Integer.compare(o1.startUIndex, o2.startUIndex);
				}
			});
			FeatureAnalysis featureAnalysis = new FeatureAnalysis();
			featureAnalysis.featureName = featureName;
			featureAnalysis.featureCodonLabel = featureCodonLabels;
			featureNameToAnalysis.put(featureName, featureAnalysis);
		}
 		
		return new WebAnalysisResult(
				new ArrayList<FeatureAnalysis>(featureNameToAnalysis.values()),
				new ArrayList<ReferenceAnalysis>(refNameToAnalysis.values()),
				new ArrayList<SequenceAnalysis>(fastaIdToSequenceAnalysis.values()));
	}

	private List<QueryAlignedSegment> generateSequenceTargetAlignment(
			CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter, String fastaID,
			DNASequence sequence, String targetRefName) {
		AlignerResult alignerResult = fastaSequenceReporter.alignToTargetReference(cmdContext, 
				targetRefName, fastaID, sequence);
		List<QueryAlignedSegment> queryToTargetRefSegsUnmerged = alignerResult.getQueryIdToAlignedSegments().get(fastaID);
		List<QueryAlignedSegment> queryToTargetRefSegs = QueryAlignedSegment.mergeAbutting(queryToTargetRefSegsUnmerged, QueryAlignedSegment.mergeAbuttingFunction());
		return queryToTargetRefSegs;
	}

	private List<AaSegment> generateAaSegs(List<TranslatedQueryAlignedSegment> translatedQueryToRefSegs, List<QueryAlignedSegment> refToUSegs) {
		TIntCharMap queryNtToAa = new TIntCharHashMap();
		if(translatedQueryToRefSegs.isEmpty()) {
			return new ArrayList<AaSegment>();
		}
		List<QueryAlignedSegment> queryToRefSegs = new ArrayList<QueryAlignedSegment>();
		for(TranslatedQueryAlignedSegment translatedQaSeg: translatedQueryToRefSegs) {
			QueryAlignedSegment queryAlignedSegment = translatedQaSeg.getQueryAlignedSegment();
			queryToRefSegs.add(queryAlignedSegment);
			int queryNt = queryAlignedSegment.getQueryStart();
			String translation = translatedQaSeg.getTranslation();
			for(int i = 0; i < translation.length(); i++) {
				queryNtToAa.put(queryNt, translation.charAt(i));
				queryNt+=3;
			}
		}
		int codonAlignedQueryNT = queryToRefSegs.get(0).getQueryStart();
		List<QueryAlignedSegment> queryToUSegs = QueryAlignedSegment.translateSegments(queryToRefSegs, refToUSegs);
		List<QueryAlignedSegment> queryToUSegsCodonAligned = TranslationUtils.truncateToCodonAlignedQuery(codonAlignedQueryNT, queryToUSegs);
		
		List<AaSegment> aaSegs = new ArrayList<AaSegment>();
		for(QueryAlignedSegment queryToUSeg: queryToUSegsCodonAligned) {
			StringBuffer translationBuf = new StringBuffer();
			AaSegment aaSeg = new AaSegment();
			aaSeg.startUIndex = queryToUSeg.getRefStart();
			aaSeg.endUIndex = queryToUSeg.getRefEnd();
			for(int i = queryToUSeg.getQueryStart(); i <= queryToUSeg.getQueryEnd()-2; i += 3) {
				translationBuf.append(queryNtToAa.get(i));
			}
			aaSeg.aaTranslation = translationBuf.toString();
			aaSegs.add(aaSeg);
		}
		return aaSegs;
	}
	
	public static abstract class Key {}
	
	public static class ReferenceKey extends Key {
		private String refName;
		public ReferenceKey(String refName) {
			super();
			this.refName = refName;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((refName == null) ? 0 : refName.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReferenceKey other = (ReferenceKey) obj;
			if (refName == null) {
				if (other.refName != null)
					return false;
			} else if (!refName.equals(other.refName))
				return false;
			return true;
		}
	}

	private static class QueryKey extends Key {
		private String fastaID;

		public QueryKey(String fastaID) {
			super();
			this.fastaID = fastaID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((fastaID == null) ? 0 : fastaID.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QueryKey other = (QueryKey) obj;
			if (fastaID == null) {
				if (other.fastaID != null)
					return false;
			} else if (!fastaID.equals(other.fastaID))
				return false;
			return true;
		}
		
	}


	
}
