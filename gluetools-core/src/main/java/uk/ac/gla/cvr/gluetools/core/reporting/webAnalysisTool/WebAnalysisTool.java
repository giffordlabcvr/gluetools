package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import gnu.trove.map.TIntCharMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntCharHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
			List<String> ancestorRefNames = new ArrayList<String>();
			for(int i = ancestors.size()-1; i >= 0; i--) {
				Alignment ancestor = ancestors.get(i);
				ReferenceSequence ancRefSeq = ancestor.getRefSequence();
				String refName = ancRefSeq.getName();
				ancestorRefNames.add(refName);
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
				ancestorRefNames.add(targetRefName);
				if(!refNameToAnalysis.containsKey(targetRefName)) {
					refNameToAnalysis.put(targetRefName, 
						new ReferenceAnalysis(targetRef, tipAlmt, tipAlmtMember));
				}
			}
			SequenceAnalysis sequenceAnalysis = new SequenceAnalysis(fastaId, new FastaSequenceObject(fastaId, sequence.getSequenceAsString()), targetRefName);
			sequenceAnalysis.ancestorRefName = ancestorRefNames;
			fastaIdToSequenceAnalysis.put(fastaId, sequenceAnalysis);
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
		
		
		Map<String, FeatureAnalysis> featureNameToAnalysis = new LinkedHashMap<String, FeatureAnalysis>();
		initFeatureAnalysis(cmdContext, featureNameToAnalysis, refNameToAnalysis.keySet(), allColsAlmt);

		
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
					List<Aa> aas = generateAas(translatedQaSegs, refToUSegs);
					
					SequenceFeatureAnalysis sequenceFeatureAnalysis = new SequenceFeatureAnalysis();
					sequenceFeatureAnalysis.featureName = featureName;
					sequenceFeatureAnalysis.aas = aas;
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
			
			List<QueryAlignedSegment> refToUSegs = allColsAlmt.getSegments(new ReferenceKey(seqAnalysis.targetRefName));
			
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
					List<Aa> aas = generateAas(translatedQaSegs, refToUSegs);
					SequenceFeatureAnalysis sequenceFeatureAnalysis = new SequenceFeatureAnalysis();
					sequenceFeatureAnalysis.featureName = featureName;
					sequenceFeatureAnalysis.aas = aas;
					sequenceFeatureAnalyses.add(sequenceFeatureAnalysis);
				}
			}
			seqAnalysis.sequenceFeatureAnalysis = sequenceFeatureAnalyses;
		}

 		
		return new WebAnalysisResult(
				new ArrayList<FeatureAnalysis>(featureNameToAnalysis.values()),
				new ArrayList<ReferenceAnalysis>(refNameToAnalysis.values()),
				new ArrayList<SequenceAnalysis>(fastaIdToSequenceAnalysis.values()));
	}

	public void initFeatureAnalysis(
			CommandContext cmdContext,
			Map<String, FeatureAnalysis> featureNameToAnalysis, 
			Collection<String> refNames,
			AllColumnsAlignment<Key> allColsAlmt) {
		
		for(FeatureAnalysisHint featureAnalysisHint: featureAnalysisHints) {
			String featureName = featureAnalysisHint.getFeatureName();
			
			// this map is used to prevent overlapping codon labels.
			TIntObjectMap<CodonLabel> uIndexToCodonLabel = new TIntObjectHashMap<CodonLabel>();
			
			Integer startUIndex = Integer.MAX_VALUE;
			Integer endUIndex = Integer.MIN_VALUE;
			
			for(String refName: refNames) {
				FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class,
						FeatureLocation.pkMap(refName, featureName), false);
				if(featureLoc != null) {
					List<ReferenceSegment> featureLocReferenceSegments = featureLoc.segmentsAsReferenceSegments();
					List<QueryAlignedSegment> featureLocQaSegments = featureLocReferenceSegments.stream()
							.map(seg -> new QueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getRefStart(), seg.getRefEnd()))
							.collect(Collectors.toList());
					List<QueryAlignedSegment> refToUSegs = allColsAlmt.getSegments(new ReferenceKey(refName));
					List<QueryAlignedSegment> featureLocRefToUSegs = QueryAlignedSegment.translateSegments(featureLocQaSegments, refToUSegs);
					
					startUIndex = Math.min(startUIndex, ReferenceSegment.minRefStart(featureLocRefToUSegs));
					endUIndex = Math.max(endUIndex, ReferenceSegment.maxRefEnd(featureLocRefToUSegs));
					
					List<LabeledCodon> labeledCodons = featureLoc.getLabeledCodons(cmdContext);

					List<CodonQueryAlignedSegment> codonRefQaSegs = new ArrayList<CodonQueryAlignedSegment>();

					for(LabeledCodon labeledCodon: labeledCodons) {
						int refStart = labeledCodon.getNtStart();
						int refEnd = refStart+2;
						CodonLabel codonLabel = new CodonLabel();
						codonLabel.label = labeledCodon.getCodonLabel();
						codonLabel.startUIndex = Integer.MAX_VALUE;
						codonLabel.endUIndex = Integer.MIN_VALUE;
						codonRefQaSegs.add(new CodonQueryAlignedSegment(codonLabel, 
								refStart, refEnd, refStart, refEnd));
					}
					List<CodonQueryAlignedSegment> codonUQaSegs = QueryAlignedSegment.translateSegments(codonRefQaSegs, featureLocRefToUSegs);
					for(CodonQueryAlignedSegment codonUQaSeg: codonUQaSegs) {
						for(int i = codonUQaSeg.getRefStart(); i <= codonUQaSeg.getRefEnd(); i++) {
							CodonLabel codonLabel = codonUQaSeg.codonLabel;
							codonLabel.startUIndex = Math.min(codonLabel.startUIndex, i);
							codonLabel.endUIndex = Math.max(codonLabel.endUIndex, i);
							if(!uIndexToCodonLabel.containsKey(i)) {
								uIndexToCodonLabel.put(i, codonLabel);
							}
						}
					}
				}
			}
			FeatureAnalysis featureAnalysis = new FeatureAnalysis();
			featureAnalysis.featureName = featureName;
			featureAnalysis.startUIndex = startUIndex;
			featureAnalysis.endUIndex = endUIndex;
			List<CodonLabel> codonLabels = new ArrayList<CodonLabel>(
					new LinkedHashSet<CodonLabel>(uIndexToCodonLabel.valueCollection()));
			Collections.sort(codonLabels, new Comparator<CodonLabel>() {
				@Override
				public int compare(CodonLabel o1, CodonLabel o2) {
					return Integer.compare(o1.startUIndex, o2.startUIndex);
				}
			});
			featureAnalysis.codonLabel = codonLabels;
			featureNameToAnalysis.put(featureName, featureAnalysis);
		}
	}

	private List<QueryAlignedSegment> generateSequenceTargetAlignment(
			CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter, String fastaID,
			DNASequence sequence, String targetRefName) {
		AlignerResult alignerResult = fastaSequenceReporter.alignToTargetReference(cmdContext, 
				targetRefName, fastaID, sequence);
		List<QueryAlignedSegment> queryToTargetRefSegsUnmerged = alignerResult.getQueryIdToAlignedSegments().get(fastaID);
		List<QueryAlignedSegment> queryToTargetRefSegs = 
				QueryAlignedSegment.mergeAbutting(queryToTargetRefSegsUnmerged, 
						QueryAlignedSegment.mergeAbuttingFunction(), 
						QueryAlignedSegment.abutsPredicate());
		return queryToTargetRefSegs;
	}

	private List<Aa> generateAas(List<TranslatedQueryAlignedSegment> translatedQueryToRefSegs, 
			List<QueryAlignedSegment> refToUSegs) {
		if(translatedQueryToRefSegs.isEmpty()) {
			return new ArrayList<Aa>();
		}
		List<AaQueryAlignedSegment> aaQueryToRefSegs = new ArrayList<AaQueryAlignedSegment>();
		for(TranslatedQueryAlignedSegment translatedQaSeg: translatedQueryToRefSegs) {
			QueryAlignedSegment queryAlignedSegment = translatedQaSeg.getQueryAlignedSegment();
			int queryNt = queryAlignedSegment.getQueryStart();
			int refNt = queryAlignedSegment.getRefStart();
			String translation = translatedQaSeg.getTranslation();
			for(int i = 0; i < translation.length(); i++) {
				Aa aa = new Aa();
				aa.aa = translation.substring(i, i+1);
				aa.startUIndex = Integer.MAX_VALUE;
				aa.endUIndex = Integer.MIN_VALUE;
				aaQueryToRefSegs.add(new AaQueryAlignedSegment(aa, 
						queryNt, queryNt+2, refNt, refNt+2));
				queryNt+=3;
			}
		}
		List<AaQueryAlignedSegment> aaQueryToUSegs = QueryAlignedSegment.translateSegments(aaQueryToRefSegs, refToUSegs);
		TIntObjectMap<Aa> uIndexToAa = new TIntObjectHashMap<Aa>();
		for(AaQueryAlignedSegment aaQueryToUSeg: aaQueryToUSegs) {
			Aa aa = aaQueryToUSeg.aa;
			for(int i = aaQueryToUSeg.getRefStart(); i <= aaQueryToUSeg.getRefEnd(); i++) {
				if(!uIndexToAa.containsKey(i)) {
					aa.startUIndex = Math.min(aa.startUIndex, i);
					aa.endUIndex = Math.max(aa.endUIndex, i);
					uIndexToAa.put(i, aa);
				}
			}
		}
		List<Aa> aas = new ArrayList<Aa>(new LinkedHashSet<Aa>(uIndexToAa.valueCollection()));
		Collections.sort(aas, new Comparator<Aa>() {
			@Override
			public int compare(Aa o1, Aa o2) {
				return Integer.compare(o1.startUIndex, o2.startUIndex);
			}
		});
		return aas;
	}
	
	public static abstract class Key {}
	
	private class CodonQueryAlignedSegment extends QueryAlignedSegment {

		CodonLabel codonLabel;
		
		public CodonQueryAlignedSegment(
				CodonLabel codonLabel, 
				int refStart, int refEnd,
				int queryStart, int queryEnd) {
			super(refStart, refEnd, queryStart, queryEnd);
			this.codonLabel = codonLabel;
		}

		@Override
		public CodonQueryAlignedSegment clone() {
			return new CodonQueryAlignedSegment(codonLabel, getRefStart(), getRefEnd(), getQueryStart(), getQueryEnd());
		}
	}

	private class AaQueryAlignedSegment extends QueryAlignedSegment {

		Aa aa;
		
		public AaQueryAlignedSegment(
				Aa aa, 
				int refStart, int refEnd,
				int queryStart, int queryEnd) {
			super(refStart, refEnd, queryStart, queryEnd);
			this.aa = aa;
		}

		@Override
		public AaQueryAlignedSegment clone() {
			return new AaQueryAlignedSegment(aa, getRefStart(), getRefEnd(), getQueryStart(), getQueryEnd());
		}
	}

	
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
