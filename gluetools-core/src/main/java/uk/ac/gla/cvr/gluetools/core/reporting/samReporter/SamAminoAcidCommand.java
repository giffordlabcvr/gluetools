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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TCharIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TCharIntProcedure;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SimpleNucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamFileSession;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate amino acids in a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] -r <relRefName> -f <featureName> (-p | [-l] -t <targetRefName>) -a <linkingAlmtName> [-q <minQScore>] [-g <minMapQ>] [-e <minDepth>] [-P <minAAPct>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                       SAM/BAM input file",
				"-n <samRefSense>, --samRefSense <samRefSense>              SAM ref seq sense",
				"-s <samRefName>, --samRefName <samRefName>                 Specific SAM ref seq",
				"-r <relRefName>, --relRefName <relRefName>                 Related reference sequence",
				"-f <featureName>, --featureName <featureName>              Feature to translate",
				"-p, --maxLikelihoodPlacer                                  Use ML placer module",
				"-l, --autoAlign                                            Auto-align consensus",
				"-t <targetRefName>, --targetRefName <targetRefName>        Target GLUE reference",
				"-a <linkingAlmtName>, --linkingAlmtName <linkingAlmtName>  Linking alignment",
				"-q <minQScore>, --minQScore <minQScore>                    Minimum Phred quality score",
				"-g <minMapQ>, --minMapQ <minMapQ>                          Minimum mapping quality score",
				"-e <minDepth>, --minDepth <minDepth>                       Minimum depth",
				"-P <minAAPct>, --minAAPct <minAAPct>                       Minimum AA percentage",

		},
		furtherHelp = 
			"This command translates a SAM/BAM file reads to amino acids. "+
			"If <samRefName> is supplied, the translated reads are limited to those which are aligned to the "+
			"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
			"file only names a single reference sequence.\n"+
			"The translation is based on a 'target' GLUE reference sequence. "+
			"The <samRefSense> may be FORWARD or REVERSE_COMPLEMENT, indicating the presumed sense of the SAM reference, relative to the GLUE references."+
			"If the --maxLikelihoodPlacer option is used, an ML placement is performed, and the target reference is "+
			"identified as the closest according to this placement. "+
			"Otherwise, the target reference must be specified using <targetRefName>."+
			"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
			"is the reference sequence  mentioned in the SAM file. "+
			"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
			"consensus and the target GLUE reference. \n"+
			"The --autoAlign option is implicit if --maxLikelihoodPlacer is used. "+
			"The target reference sequence must be a member of the "+
			"'linking alignment', specified by <linkingAlmtName>. "+
	        "The <relRefName> argument specifies the related reference sequence, on which the feature is defined. "+
			"If the linking alignment is constrained, the related reference must constrain an ancestor alignment "+
	        "of the linking alignment. Otherwise, it may be any reference sequence which shares membership of the "+
			"linking alignment with the target reference. "+
			"The <featureName> arguments specifies a feature location on the related reference. "+
			"The translated amino acids will be limited to the specified feature location.\n"+
			"Reads will not contribute to the translation at a given codon location if any reported nucleotide quality score at that location is less than "+
			"<minQScore> (default value is derived from the module config). \n"+
			"No result will be generated for a codon location if the number of contributing reads is less than <minDepth> "+
			"(default value is derived from the module config).\n"+
			"Amino acid values will only display in the result if the percentage of reads contributing that value is at least <minAAPct> (default 0).",

		metaTags = {CmdMeta.consoleOnly}	
)
public class SamAminoAcidCommand extends ReferenceLinkedSamReporterCommand<SamAminoAcidResult> 
	implements ProvidedProjectModeCommand, SamPairedParallelProcessor<SamAminoAcidCommand.SamAminoAcidContext, TIntObjectMap<SamAminoAcidCommand.AminoAcidReadCount>> {


	public static final String MIN_AA_PCT = "minAAPct";

	private Double minAAPct;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.minAAPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_AA_PCT, 0.0, true, 100.0, true, false)).orElse(0.0);
	}


	@Override
	protected SamAminoAcidResult execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		SamRefInfo samRefInfo = getSamRefInfo(consoleCmdContext, samReporter);
		ValidationStringency validationStringency = samReporter.getSamReaderValidationStringency();
		String samFileName = getFileName();

		try(SamFileSession samFileSession = SamReporterPreprocessor.preprocessSam(consoleCmdContext, samFileName, validationStringency)) {
			DNASequence consensusSequence = null;
			ReferenceSequence targetRef;
			if(useMaxLikelihoodPlacer()) {
				Map<String, DNASequence> consensusMap = SamUtils.getSamConsensus(consoleCmdContext, samFileName, samFileSession, 
						validationStringency, getSuppliedSamRefName(),"samConsensus", getMinQScore(samReporter), getMinMapQ(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
				consensusSequence = consensusMap.get("samConsensus");
				AlignmentMember targetRefAlmtMember = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensusSequence);
				targetRef = targetRefAlmtMember.targetReferenceFromMember();
				samReporter.log(Level.FINE, "Max likelihood placement of consensus sequence selected target reference "+targetRef.getName());
			} else {
				targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
						ReferenceSequence.pkMap(getTargetRefName()));
			}
			Alignment linkingAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, 
					Alignment.pkMap(getLinkingAlmtName()));
			ReferenceSequence relatedRef = linkingAlmt.getRelatedRef(cmdContext, getRelatedRefName());

			FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getRelatedRefName(), getFeatureName()), false);
			Feature feature = featureLoc.getFeature();
			feature.checkCodesAminoAcids();

			List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, samFileSession, consoleCmdContext, targetRef, consensusSequence);

			AlignmentMember linkingAlmtMember = targetRef.getLinkingAlignmentMembership(getLinkingAlmtName());

			// translate segments to linking alignment coords
			List<QueryAlignedSegment> samRefToLinkingAlmtSegs = linkingAlmt.translateToAlmt(cmdContext, 
					linkingAlmtMember.getSequence().getSource().getName(), linkingAlmtMember.getSequence().getSequenceID(), 
					samRefToTargetRefSegs);

			// translate segments to related reference
			List<QueryAlignedSegment> samRefToRelatedRefSegsFull = linkingAlmt.translateToRelatedRef(cmdContext, samRefToLinkingAlmtSegs, relatedRef);

			List<LabeledCodonReferenceSegment> labeledCodonReferenceSegments = featureLoc.getLabeledCodonReferenceSegments(cmdContext);

			// trim down to the feature area.
			List<QueryAlignedSegment> samRefToRelatedRefSegsUnmerged = 
					ReferenceSegment.intersection(samRefToRelatedRefSegsFull, labeledCodonReferenceSegments, ReferenceSegment.cloneLeftSegMerger());

			List<QueryAlignedSegment> samRefToRelatedRefSegs = QueryAlignedSegment.mergeAbutting(samRefToRelatedRefSegsUnmerged, 
					QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
					QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

			if(samRefToRelatedRefSegs.isEmpty()) {
				return new SamAminoAcidResult(Collections.emptyList());
			}

			SamRefSense samRefSense = getSamRefSense(samReporter);

			TIntObjectMap<LabeledCodon> relatedRefNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);

			// build a map from related ref NT to AA read count.
			TIntObjectHashMap<AminoAcidReadCount> relatedRefNtToAminoAcidReadCount = new TIntObjectHashMap<AminoAcidReadCount>();
			List<Integer> mappedRelatedRefNts = new ArrayList<Integer>();
			for(QueryAlignedSegment qaSeg: samRefToRelatedRefSegs) {
				for(int relatedRefNt = qaSeg.getRefStart(); relatedRefNt <= qaSeg.getRefEnd(); relatedRefNt++) {
					LabeledCodon labeledCodon = relatedRefNtToLabeledCodon.get(relatedRefNt);
					if(labeledCodon != null) {
						mappedRelatedRefNts.add(relatedRefNt);
						int samRefNt = relatedRefNt + qaSeg.getReferenceToQueryOffset();
						int resultSamRefNt = samRefNt;
						if(samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
							// we want to report results in the SAM file's own coordinates.
							resultSamRefNt = ReferenceSegment.reverseLocationSense(samRefInfo.getSamRefLength(), samRefNt);
						}
						relatedRefNtToAminoAcidReadCount.put(relatedRefNt, new AminoAcidReadCount(labeledCodon, resultSamRefNt));
					}
				}
			}

			// translate reads.
			final Translator translator = new CommandContextTranslator(cmdContext);

			Supplier<SamAminoAcidContext> contextSupplier = () -> {
				SamAminoAcidContext context = new SamAminoAcidContext();
				context.samReporter = samReporter;
				context.cmdContext = cmdContext;
				context.translator = translator;
				context.samRefInfo = samRefInfo;
				context.samRefSense = samRefSense;
				context.featureLoc = featureLoc;
				// clone these segments
				synchronized(samRefToRelatedRefSegs) {
					context.samRefToRelatedRefSegs = QueryAlignedSegment.cloneList(samRefToRelatedRefSegs);
				}
				// clone the table
				synchronized(relatedRefNtToAminoAcidReadCount) {
					context.relatedRefNtToAminoAcidReadCount = new TIntObjectHashMap<AminoAcidReadCount>();
					for(int key: relatedRefNtToAminoAcidReadCount.keys()) {
						AminoAcidReadCount aaReadCount = relatedRefNtToAminoAcidReadCount.get(key);
						context.relatedRefNtToAminoAcidReadCount.put(key, new AminoAcidReadCount(aaReadCount.labeledCodon, aaReadCount.samRefNt));
					}
				}
				return context;
			};
			TIntObjectMap<AminoAcidReadCount> mergedResult = SamUtils.pairedParallelSamIterate(contextSupplier, consoleCmdContext, samFileSession, 
					validationStringency, this);


			final List<LabeledAminoAcidReadCount> rowData = new ArrayList<LabeledAminoAcidReadCount>();

			for(Integer relatedRefNt: mappedRelatedRefNts) {
				AminoAcidReadCount aminoAcidReadCount = mergedResult.get(relatedRefNt);
				if(aminoAcidReadCount.totalReadsAtCodon <= getMinDepth(samReporter)) {
					continue;
				}
				aminoAcidReadCount.aaToReadCount.forEachEntry(new TCharIntProcedure() {
					@Override
					public boolean execute(char aminoAcid, int numReads) {
						double percentReadsWithAminoAcid = 100.0 * numReads / (double) aminoAcidReadCount.totalReadsAtCodon;
						rowData.add(new LabeledAminoAcidReadCount(
								aminoAcidReadCount.labeledCodon, 
								new String(new char[]{aminoAcid}),
								aminoAcidReadCount.samRefNt, 
								numReads, percentReadsWithAminoAcid));
						return true;
					}
				});
			}

			List<LabeledAminoAcidReadCount> rowDataFiltered = rowData.stream()
					.filter(row -> row.getPercentReadsWithAminoAcid() >= minAAPct)
					.collect(Collectors.toList());

			return new SamAminoAcidResult(rowDataFiltered);
		}
	 
	}



	
	private TIntObjectMap<AminoAcidWithQuality> translateReadWithQualityScores(SamAminoAcidContext context, SAMRecord samRecord) {

		List<QueryAlignedSegment> readToSamRefSegs = context.samReporter.getReadToSamRefSegs(samRecord);
		String readString = samRecord.getReadString().toUpperCase();
		String qualityString = samRecord.getBaseQualityString();
		if(context.samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
			readToSamRefSegs = QueryAlignedSegment.reverseSense(readToSamRefSegs, readString.length(), context.samRefInfo.getSamRefLength());
			readString = FastaUtils.reverseComplement(readString);
			qualityString = StringUtils.reverseString(qualityString);
		}
		List<QueryAlignedSegment> readToRelatedRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, context.samRefToRelatedRefSegs);
		
		List<LabeledQueryAminoAcid> labeledReadAas = context.featureLoc.translateQueryNucleotides(context.cmdContext,
				context.translator, readToRelatedRefSegs, new SimpleNucleotideContentProvider(readString));
		
		TIntObjectMap<AminoAcidWithQuality> refNtToAminoAcidWithQuality = new TIntObjectHashMap<SamAminoAcidCommand.AminoAcidWithQuality>();
		
		int minQScore = getMinQScore(context.samReporter); 
		// put the AAs resulting from translating the read into the map. 
		for(LabeledQueryAminoAcid labeledReadAa: labeledReadAas) {
			int worstQual;
			char qualityChar1 = SegmentUtils.base1Char(qualityString, labeledReadAa.getQueryNtStart());
			worstQual = SamUtils.qualityCharToQScore(qualityChar1);
			if(SamUtils.qualityCharToQScore(qualityChar1) < minQScore) {
				continue;
			} 
			char qualityChar2 = SegmentUtils.base1Char(qualityString, labeledReadAa.getQueryNtMiddle());
			worstQual = Math.min(worstQual, SamUtils.qualityCharToQScore(qualityChar2));
			if(SamUtils.qualityCharToQScore(qualityChar2) < minQScore) {
				continue;
			} 
			char qualityChar3 = SegmentUtils.base1Char(qualityString, labeledReadAa.getQueryNtEnd());
			worstQual = Math.min(worstQual, SamUtils.qualityCharToQScore(qualityChar3));
			if(SamUtils.qualityCharToQScore(qualityChar3) < minQScore) {
				continue;
			} 
			refNtToAminoAcidWithQuality.put(labeledReadAa.getLabeledAminoAcid().getLabeledCodon().getNtStart(), 
					new AminoAcidWithQuality(labeledReadAa.getLabeledAminoAcid().getAminoAcid().charAt(0), worstQual));
		}
		return refNtToAminoAcidWithQuality;
	}
	
	private class AminoAcidWithQuality {
		char aa;
		int worstCodonQuality;

		public AminoAcidWithQuality(char aa, int worstCodonQuality) {
			super();
			this.aa = aa;
			this.worstCodonQuality = worstCodonQuality;
		}
	}
	
	public static class AminoAcidReadCount {
		private LabeledCodon labeledCodon;
		private int samRefNt;
		private int totalReadsAtCodon = 0;
		TCharIntMap aaToReadCount = new TCharIntHashMap();
		
		public AminoAcidReadCount(LabeledCodon labeledCodon, int samRefNt) {
			super();
			this.labeledCodon = labeledCodon;
			this.samRefNt = samRefNt;
		}
		public void addAaRead(char aaChar) {
			aaToReadCount.adjustOrPutValue(aaChar, 1, 1);
			totalReadsAtCodon++;
		}
	}

	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {
		public Completer() {
			super();
			registerEnumLookup("samRefSense", SamRefSense.class);
			registerVariableInstantiator("samRefName", new SamRefNameInstantiator());
		}
	}

	

	@Override
	public void processPair(SamAminoAcidContext context, SAMRecord read1, SAMRecord read2) {
		if(!context.samRecordFilter.recordPasses(read1)) {
			processSingleton(context, read2);
		} else if(!context.samRecordFilter.recordPasses(read2)) {
			processSingleton(context, read1);
		} else {
			TIntObjectMap<AminoAcidWithQuality> read1TranslationWithQuals = translateReadWithQualityScores(context, read1);
			TIntObjectMap<AminoAcidWithQuality> read2TranslationWithQuals = translateReadWithQualityScores(context, read2);

			int read1MapQ = read1.getMappingQuality();
			int read2MapQ = read2.getMappingQuality();
			int readNameHashCoinFlip = Math.abs(read1.getReadName().hashCode()) % 2;

			for(int relatedRefNt : read1TranslationWithQuals.keys()) {
				AminoAcidReadCount aminoAcidReadCount = context.relatedRefNtToAminoAcidReadCount.get(relatedRefNt);
				AminoAcidWithQuality read1AaWithQual = read1TranslationWithQuals.get(relatedRefNt);
				AminoAcidWithQuality read2AaWithQual = read2TranslationWithQuals.remove(relatedRefNt);
				if(read2AaWithQual == null) {
					aminoAcidReadCount.addAaRead(read1AaWithQual.aa);
				} else {
					int read1qual = read1AaWithQual.worstCodonQuality;
					int read2qual = read2AaWithQual.worstCodonQuality;
					if(read1qual < read2qual) {
						aminoAcidReadCount.addAaRead(read2AaWithQual.aa);
					} else if(read1qual > read2qual) {
						aminoAcidReadCount.addAaRead(read1AaWithQual.aa);
					} else if(read1MapQ != 255 && read2MapQ != 255 && read1MapQ < read2MapQ) {
						aminoAcidReadCount.addAaRead(read2AaWithQual.aa);
					} else if(read1MapQ != 255 && read2MapQ != 255 && read1MapQ > read2MapQ) {
						aminoAcidReadCount.addAaRead(read1AaWithQual.aa);
					} else if(readNameHashCoinFlip == 0) {
						aminoAcidReadCount.addAaRead(read1AaWithQual.aa);
					} else {
						aminoAcidReadCount.addAaRead(read1AaWithQual.aa);
					}
				}
			}
			for(int relatedRefNt : read2TranslationWithQuals.keys()) {
				AminoAcidReadCount aminoAcidReadCount = context.relatedRefNtToAminoAcidReadCount.get(relatedRefNt);
				aminoAcidReadCount.addAaRead(read2TranslationWithQuals.get(relatedRefNt).aa);
			}
		}
	}

	@Override
	public void processSingleton(SamAminoAcidContext context, SAMRecord read) {
		if(context.samRecordFilter.recordPasses(read)) {
			TIntObjectMap<AminoAcidWithQuality> readTranslationWithQuals = translateReadWithQualityScores(context, read);
			for(int relatedRefNt : readTranslationWithQuals.keys()) {
				AminoAcidReadCount aminoAcidReadCount = context.relatedRefNtToAminoAcidReadCount.get(relatedRefNt);
				aminoAcidReadCount.addAaRead(readTranslationWithQuals.get(relatedRefNt).aa);
			}
		}
	}


	public static class SamAminoAcidContext {
		CommandContext cmdContext;
		SamReporter samReporter;
		SamRefInfo samRefInfo;
		List<QueryAlignedSegment> samRefToRelatedRefSegs;
		SamRefSense samRefSense;
		TIntObjectMap<AminoAcidReadCount> relatedRefNtToAminoAcidReadCount;
		Translator translator;
		FeatureLocation featureLoc;
		SamRecordFilter samRecordFilter;
	}



	@Override
	public void initContextForReader(SamAminoAcidContext context, SamReader samReader) {
		context.samRecordFilter = 
				new SamUtils.ConjunctionBasedRecordFilter(
						new SamUtils.ReferenceBasedRecordFilter(samReader, getFileName(), getSuppliedSamRefName()), 
						new SamUtils.MappingQualityRecordFilter(getMinMapQ(context.samReporter))
				);
	}


	@Override
	public TIntObjectMap<AminoAcidReadCount> contextResult(SamAminoAcidContext context) {
		return context.relatedRefNtToAminoAcidReadCount;
	}


	@Override
	public TIntObjectMap<AminoAcidReadCount> reduceResults(
			TIntObjectMap<AminoAcidReadCount> result1,
			TIntObjectMap<AminoAcidReadCount> result2) {
		TIntObjectMap<AminoAcidReadCount> reduced = new TIntObjectHashMap<SamAminoAcidCommand.AminoAcidReadCount>();
		for(int key : result1.keys()) {
			AminoAcidReadCount aaReadCount1 = result1.get(key);
			AminoAcidReadCount aaReadCount2 = result2.get(key);
			
			AminoAcidReadCount resultReadCount = new AminoAcidReadCount(aaReadCount1.labeledCodon, aaReadCount1.samRefNt);
			resultReadCount.totalReadsAtCodon = aaReadCount1.totalReadsAtCodon + aaReadCount2.totalReadsAtCodon;
			resultReadCount.aaToReadCount = new TCharIntHashMap();
			for(char aa: aaReadCount1.aaToReadCount.keys()) {
				int readCount1 = aaReadCount1.aaToReadCount.get(aa);
				resultReadCount.aaToReadCount.adjustOrPutValue(aa, readCount1, readCount1);
			}
			for(char aa: aaReadCount2.aaToReadCount.keys()) {
				int readCount2 = aaReadCount2.aaToReadCount.get(aa);
				resultReadCount.aaToReadCount.adjustOrPutValue(aa, readCount2, readCount2);
			}
			reduced.put(key, resultReadCount);
		}
		return reduced;
	}



	
}
