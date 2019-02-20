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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAminoAcidAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SimpleNucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamReporterPreprocessorSession;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

@CommandClass(
		commandWords={"codon-triplets"}, 
		description = "Analyse different codon triplets in a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] (-m <selectorName> | -r <relRefName> -f <featureName> [-c <lcStart> <lcEnd>]) (-p | [-l] -t <targetRefName>) -a <linkingAlmtName> [-q <minQScore>] [-g <minMapQ>] [-e <minDepth>] [-P <minTripletPct>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                       SAM/BAM input file",
				"-n <samRefSense>, --samRefSense <samRefSense>              SAM ref seq sense",
				"-s <samRefName>, --samRefName <samRefName>                 Specific SAM ref seq",
				"-m <selectorName>, --selectorName <selectorName>           Column selector module",
				"-r <relRefName>, --relRefName <relRefName>                 Related reference sequence",
				"-f <featureName>, --featureName <featureName>              Feature to analyse",
				"-c, --labelledCodon                                        Region between codon labels",
				"-p, --maxLikelihoodPlacer                                  Use ML placer module",
				"-l, --autoAlign                                            Auto-align consensus",
				"-t <targetRefName>, --targetRefName <targetRefName>        Target GLUE reference",
				"-a <linkingAlmtName>, --linkingAlmtName <linkingAlmtName>  Linking alignment",
				"-q <minQScore>, --minQScore <minQScore>                    Minimum Phred quality score",
				"-g <minMapQ>, --minMapQ <minMapQ>                          Minimum mapping quality score",
				"-e <minDepth>, --minDepth <minDepth>                       Minimum depth",
				"-P <minTripletPct>, --minTripletPct <minTripletPct>        Minimum triplet percentage",

		},
		furtherHelp = 
			"This command analyses codon nucleotide triplets in a SAM/BAM file. "+
			"If <samRefName> is supplied, the reads are limited to those which are aligned to the "+
			"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
			"file only names a single reference sequence.\n"+
			"The analysis is based on a 'target' GLUE reference sequence. "+
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
			"The analysis will be limited to the specified feature location.\n"+
			"Reads will not contribute to the analysis at a given codon location if any reported nucleotide quality score at that location is less than "+
			"<minQScore> (default value is derived from the module config). \n"+
			"No result will be generated for a codon location if the number of contributing reads is less than <minDepth> "+
			"(default value is derived from the module config).\n"+
			"Triplet values will only display in the result if the percentage of reads contributing that value is at least <minTripletPct> (default 0).",

		metaTags = {CmdMeta.consoleOnly}	
)
public class SamCodonTripletsCommand extends ReferenceLinkedSamReporterCommand<SamCodonTripletsResult> 
	implements ProvidedProjectModeCommand, SamPairedParallelProcessor<SamCodonTripletsCommand.SamCodonTripletsContext, TIntObjectMap<SamCodonTripletsCommand.TripletReadCount>> {

	// TODO -- too much copy / paste: this really should share a base class with SamAminoAcidCommand.
	

	public static final String MIN_TRIPLET_PCT = "minTripletPct";

	private Double minTripletPct;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.minTripletPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_TRIPLET_PCT, 0.0, true, 100.0, true, false)).orElse(0.0);
		if(this.getNtRegion()) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Illegal option --ntRegion");
		}
	}


	@Override
	protected SamCodonTripletsResult execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		SamRefInfo samRefInfo = getSamRefInfo(consoleCmdContext, samReporter);
		ValidationStringency validationStringency = samReporter.getSamReaderValidationStringency();
		String samFileName = getFileName();

		try(SamReporterPreprocessorSession samReporterPreprocessorSession = SamReporterPreprocessor.getPreprocessorSession(consoleCmdContext, samFileName, samReporter)) {
			ReferenceSequence targetRef;
			if(useMaxLikelihoodPlacer()) {
				targetRef = samReporterPreprocessorSession.getTargetRefBasedOnPlacer(consoleCmdContext, samReporter, this);
			} else {
				targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
						ReferenceSequence.pkMap(getTargetRefName()));
			}
			Alignment linkingAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, 
					Alignment.pkMap(getLinkingAlmtName()));

			IAminoAcidAlignmentColumnsSelector columnsSelector = getAminoAcidAlignmentColumnsSelector(cmdContext);
			columnsSelector.checkAminoAcidSelector(cmdContext);
			ReferenceSequence relatedRef = linkingAlmt.getRelatedRef(cmdContext, columnsSelector.getRelatedRefName());
			
			List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, samReporterPreprocessorSession, consoleCmdContext, targetRef);

			AlignmentMember linkingAlmtMember = targetRef.getLinkingAlignmentMembership(getLinkingAlmtName());

			// translate segments to linking alignment coords
			List<QueryAlignedSegment> samRefToLinkingAlmtSegs = linkingAlmt.translateToAlmt(cmdContext, 
					linkingAlmtMember.getSequence().getSource().getName(), linkingAlmtMember.getSequence().getSequenceID(), 
					samRefToTargetRefSegs);

			// translate segments to related reference
			List<QueryAlignedSegment> samRefToRelatedRefSegsFull = linkingAlmt.translateToRelatedRef(cmdContext, samRefToLinkingAlmtSegs, relatedRef);

			List<LabeledCodon> selectedLabeledCodons = columnsSelector.selectLabeledCodons(cmdContext);
			
			List<LabeledCodonReferenceSegment> labeledCodonReferenceSegments = new ArrayList<LabeledCodonReferenceSegment>();
			for(LabeledCodon selectedLabeledCodon: selectedLabeledCodons) {
				labeledCodonReferenceSegments.addAll(selectedLabeledCodon.getLcRefSegments());
			}

			// trim down to the feature area.
			List<QueryAlignedSegment> samRefToRelatedRefSegsUnmerged = 
					ReferenceSegment.intersection(samRefToRelatedRefSegsFull, labeledCodonReferenceSegments, ReferenceSegment.cloneLeftSegMerger());

			List<QueryAlignedSegment> samRefToRelatedRefSegs = QueryAlignedSegment.mergeAbutting(samRefToRelatedRefSegsUnmerged, 
					QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
					QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

			if(samRefToRelatedRefSegs.isEmpty()) {
				return new SamCodonTripletsResult(Collections.emptyList());
			}

			SamRefSense samRefSense = getSamRefSense(samReporter);

			TIntObjectMap<LabeledCodon> relatedRefNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();

			for(LabeledCodon labeledCodon: selectedLabeledCodons) {
				relatedRefNtToLabeledCodon.put(labeledCodon.getNtStart(), labeledCodon);
			}

			
			// build a map from related ref NT to triplet read count.
			TIntObjectHashMap<TripletReadCount> relatedRefNtToTripletReadCount = new TIntObjectHashMap<TripletReadCount>();
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
						relatedRefNtToTripletReadCount.put(relatedRefNt, new TripletReadCount(labeledCodon, resultSamRefNt));
					}
				}
			}

			// translate reads.
			final Translator translator = new CommandContextTranslator(cmdContext);

			Supplier<SamCodonTripletsContext> contextSupplier = () -> {
				SamCodonTripletsContext context = new SamCodonTripletsContext();
				context.samReporter = samReporter;
				context.cmdContext = cmdContext;
				context.translator = translator;
				context.samRefInfo = samRefInfo;
				context.samRefSense = samRefSense;
				context.featureLocRefSegs = new ArrayList<FeatureLocRefSegs>();
				synchronized(selectedLabeledCodons) {
					Map<String, FeatureLocRefSegs> featureNameToFlrs = new LinkedHashMap<String, FeatureLocRefSegs>();
					for(LabeledCodon lc: selectedLabeledCodons) {
						String featureName = lc.getFeatureName();
						FeatureLocRefSegs flrs = featureNameToFlrs.get(featureName);
						if(flrs == null) {
							flrs = new FeatureLocRefSegs();
							flrs.featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
									FeatureLocation.pkMap(columnsSelector.getRelatedRefName(), featureName));
							flrs.refSegs = new ArrayList<ReferenceSegment>();
							featureNameToFlrs.put(featureName, flrs);
						}
						List<LabeledCodonReferenceSegment> lcRefSegs = lc.getLcRefSegments();
						List<ReferenceSegment> overlaps = ReferenceSegment.intersection(flrs.refSegs, lcRefSegs, ReferenceSegment.cloneLeftSegMerger());
						List<LabeledCodonReferenceSegment> newBits = ReferenceSegment.subtract(lcRefSegs, overlaps);
						flrs.refSegs.addAll(newBits);
						ReferenceSegment.sortByRefStart(flrs.refSegs);
					}
					context.featureLocRefSegs = new ArrayList<FeatureLocRefSegs>(featureNameToFlrs.values());
					context.featureLocRefSegs.forEach(flrs -> 
						{ 
							flrs.refSegs = ReferenceSegment.mergeAbutting(flrs.refSegs, 
									ReferenceSegment.mergeAbuttingFunctionReferenceSegment(),
									ReferenceSegment.abutsPredicateReferenceSegment()); 
						} );
				}
				// clone these segments
				synchronized(samRefToRelatedRefSegs) {
					context.samRefToRelatedRefSegs = QueryAlignedSegment.cloneList(samRefToRelatedRefSegs);
				}
				// clone the table
				synchronized(relatedRefNtToTripletReadCount) {
					context.relatedRefNtToTripletReadCount = new TIntObjectHashMap<TripletReadCount>();
					for(int key: relatedRefNtToTripletReadCount.keys()) {
						TripletReadCount tripletReadCount = relatedRefNtToTripletReadCount.get(key);
						context.relatedRefNtToTripletReadCount.put(key, new TripletReadCount(tripletReadCount.labeledCodon, tripletReadCount.samRefNt));
					}
				}
				return context;
			};
			TIntObjectMap<TripletReadCount> mergedResult = SamUtils.pairedParallelSamIterate(contextSupplier, consoleCmdContext, samReporterPreprocessorSession, 
					validationStringency, this);


			final List<LabeledCodonTripletReadCount> rowData = new ArrayList<LabeledCodonTripletReadCount>();

			for(Integer relatedRefNt: mappedRelatedRefNts) {
				TripletReadCount tripletReadCount = mergedResult.get(relatedRefNt);
				if(tripletReadCount.totalReadsAtCodon <= getMinDepth(samReporter)) {
					continue;
				}
				tripletReadCount.tripletToReadCount.forEachEntry(new TObjectIntProcedure<String>() {
					@Override
					public boolean execute(String triplet, int numReads) {
						double percentReadsWithTriplet = 100.0 * numReads / (double) tripletReadCount.totalReadsAtCodon;
						rowData.add(new LabeledCodonTripletReadCount(
								tripletReadCount.labeledCodon, 
								triplet,
								TranslationUtils.translateToAaString(triplet),
								tripletReadCount.samRefNt, 
								numReads,
								tripletReadCount.totalReadsAtCodon - numReads,
								percentReadsWithTriplet));
						return true;
					}
				});
			}

			List<LabeledCodonTripletReadCount> rowDataFiltered = rowData.stream()
					.filter(row -> row.getPercentReadsWithTriplet() >= minTripletPct)
					.collect(Collectors.toList());

			return new SamCodonTripletsResult(rowDataFiltered);
		}
	 
	}



	
	private TIntObjectMap<TripletWithQuality> getReadTripletsWithQuality(SamCodonTripletsContext context, SAMRecord samRecord) {

		List<QueryAlignedSegment> readToSamRefSegs = context.samReporter.getReadToSamRefSegs(samRecord);
		String readString = samRecord.getReadString().toUpperCase();
		String qualityString = samRecord.getBaseQualityString();
		if(context.samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
			readToSamRefSegs = QueryAlignedSegment.reverseSense(readToSamRefSegs, readString.length(), context.samRefInfo.getSamRefLength());
			readString = FastaUtils.reverseComplement(readString);
			qualityString = StringUtils.reverseString(qualityString);
		}
		List<QueryAlignedSegment> readToRelatedRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, context.samRefToRelatedRefSegs);
		
		List<LabeledQueryAminoAcid> labeledReadAas = new ArrayList<LabeledQueryAminoAcid>(); 
				
		for(FeatureLocRefSegs flrs: context.featureLocRefSegs) {
			List<QueryAlignedSegment> readToRefSegsInFeature = ReferenceSegment.intersection(flrs.refSegs, readToRelatedRefSegs, ReferenceSegment.cloneRightSegMerger());
			labeledReadAas.addAll(flrs.featureLoc.translateQueryNucleotides(context.cmdContext, context.translator, readToRefSegsInFeature, new SimpleNucleotideContentProvider(readString)));
		}
		
		TIntObjectMap<TripletWithQuality> refNtToTripletWithQuality = new TIntObjectHashMap<SamCodonTripletsCommand.TripletWithQuality>();
		
		int minQScore = getMinQScore(context.samReporter); 
		// put the triplets resulting from translating the read into the map. 
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
			refNtToTripletWithQuality.put(labeledReadAa.getLabeledAminoAcid().getLabeledCodon().getNtStart(), 
					new TripletWithQuality(labeledReadAa.getLabeledAminoAcid().getTranslationInfo().getTripletNtsString(), worstQual));
		}
		return refNtToTripletWithQuality;
	}
	
	private class TripletWithQuality {
		String triplet;
		int worstCodonQuality;

		public TripletWithQuality(String triplet, int worstCodonQuality) {
			super();
			this.triplet = triplet;
			this.worstCodonQuality = worstCodonQuality;
		}
	}
	
	public static class TripletReadCount {
		private LabeledCodon labeledCodon;
		private int samRefNt;
		private int totalReadsAtCodon = 0;
		TObjectIntHashMap<String> tripletToReadCount = new TObjectIntHashMap<String>();
		
		public TripletReadCount(LabeledCodon labeledCodon, int samRefNt) {
			super();
			this.labeledCodon = labeledCodon;
			this.samRefNt = samRefNt;
		}
		public void addTripletRead(String triplet) {
			tripletToReadCount.adjustOrPutValue(triplet, 1, 1);
			totalReadsAtCodon++;
		}
	}

	
	@CompleterClass
	public static class Completer extends ReferenceLinkedSamReporterCommand.Completer {
		public Completer() {
			super();
		}
	}

	

	@Override
	public void processPair(SamCodonTripletsContext context, SAMRecord read1, SAMRecord read2) {
		if(!context.samRecordFilter.recordPasses(read1)) {
			processSingleton(context, read2);
		} else if(!context.samRecordFilter.recordPasses(read2)) {
			processSingleton(context, read1);
		} else {
			TIntObjectMap<TripletWithQuality> read1TranslationWithQuals = getReadTripletsWithQuality(context, read1);
			TIntObjectMap<TripletWithQuality> read2TranslationWithQuals = getReadTripletsWithQuality(context, read2);

			int read1MapQ = read1.getMappingQuality();
			int read2MapQ = read2.getMappingQuality();
			int readNameHashCoinFlip = Math.abs(read1.getReadName().hashCode()) % 2;

			for(int relatedRefNt : read1TranslationWithQuals.keys()) {
				TripletReadCount tripletReadCount = context.relatedRefNtToTripletReadCount.get(relatedRefNt);
				TripletWithQuality read1TripletWithQual = read1TranslationWithQuals.get(relatedRefNt);
				TripletWithQuality read2TripletWithQual = read2TranslationWithQuals.remove(relatedRefNt);
				if(read2TripletWithQual == null) {
					tripletReadCount.addTripletRead(read1TripletWithQual.triplet);
				} else {
					int read1qual = read1TripletWithQual.worstCodonQuality;
					int read2qual = read2TripletWithQual.worstCodonQuality;
					if(read1qual < read2qual) {
						tripletReadCount.addTripletRead(read2TripletWithQual.triplet);
					} else if(read1qual > read2qual) {
						tripletReadCount.addTripletRead(read1TripletWithQual.triplet);
					} else if(read1MapQ != 255 && read2MapQ != 255 && read1MapQ < read2MapQ) {
						tripletReadCount.addTripletRead(read2TripletWithQual.triplet);
					} else if(read1MapQ != 255 && read2MapQ != 255 && read1MapQ > read2MapQ) {
						tripletReadCount.addTripletRead(read1TripletWithQual.triplet);
					} else if(readNameHashCoinFlip == 0) {
						tripletReadCount.addTripletRead(read1TripletWithQual.triplet);
					} else {
						tripletReadCount.addTripletRead(read1TripletWithQual.triplet);
					}
				}
			}
			for(int relatedRefNt : read2TranslationWithQuals.keys()) {
				TripletReadCount tripletReadCount = context.relatedRefNtToTripletReadCount.get(relatedRefNt);
				tripletReadCount.addTripletRead(read2TranslationWithQuals.get(relatedRefNt).triplet);
			}
		}
	}

	@Override
	public void processSingleton(SamCodonTripletsContext context, SAMRecord read) {
		if(context.samRecordFilter.recordPasses(read)) {
			TIntObjectMap<TripletWithQuality> readTranslationWithQuals = getReadTripletsWithQuality(context, read);
			for(int relatedRefNt : readTranslationWithQuals.keys()) {
				TripletReadCount tripletReadCount = context.relatedRefNtToTripletReadCount.get(relatedRefNt);
				tripletReadCount.addTripletRead(readTranslationWithQuals.get(relatedRefNt).triplet);
			}
		}
	}


	public static class SamCodonTripletsContext {
		CommandContext cmdContext;
		SamReporter samReporter;
		SamRefInfo samRefInfo;
		List<QueryAlignedSegment> samRefToRelatedRefSegs;
		SamRefSense samRefSense;
		TIntObjectMap<TripletReadCount> relatedRefNtToTripletReadCount;
		Translator translator;
		List<FeatureLocRefSegs> featureLocRefSegs;
		SamRecordFilter samRecordFilter;
	}



	@Override
	public void initContextForReader(SamCodonTripletsContext context, SamReader samReader) {
		context.samRecordFilter = 
				new SamUtils.ConjunctionBasedRecordFilter(
						new SamUtils.ReferenceBasedRecordFilter(samReader, getFileName(), getSuppliedSamRefName()), 
						new SamUtils.MappingQualityRecordFilter(getMinMapQ(context.samReporter))
				);
	}


	@Override
	public TIntObjectMap<TripletReadCount> contextResult(SamCodonTripletsContext context) {
		return context.relatedRefNtToTripletReadCount;
	}


	@Override
	public TIntObjectMap<TripletReadCount> reduceResults(
			TIntObjectMap<TripletReadCount> result1,
			TIntObjectMap<TripletReadCount> result2) {
		TIntObjectMap<TripletReadCount> reduced = new TIntObjectHashMap<SamCodonTripletsCommand.TripletReadCount>();
		for(int key : result1.keys()) {
			TripletReadCount tripletReadCount1 = result1.get(key);
			TripletReadCount tripletReadCount2 = result2.get(key);
			
			TripletReadCount resultReadCount = new TripletReadCount(tripletReadCount1.labeledCodon, tripletReadCount1.samRefNt);
			resultReadCount.totalReadsAtCodon = tripletReadCount1.totalReadsAtCodon + tripletReadCount2.totalReadsAtCodon;
			resultReadCount.tripletToReadCount = new TObjectIntHashMap<String>();
			for(Object triplet: tripletReadCount1.tripletToReadCount.keys()) {
				int readCount1 = tripletReadCount1.tripletToReadCount.get(triplet);
				resultReadCount.tripletToReadCount.adjustOrPutValue((String) triplet, readCount1, readCount1);
			}
			for(Object triplet: tripletReadCount2.tripletToReadCount.keys()) {
				int readCount2 = tripletReadCount2.tripletToReadCount.get(triplet);
				resultReadCount.tripletToReadCount.adjustOrPutValue((String) triplet, readCount2, readCount2);
			}
			reduced.put(key, resultReadCount);
		}
		return reduced;
	}

	private class FeatureLocRefSegs {
		private FeatureLocation featureLoc;
		private List<ReferenceSegment> refSegs;
	}

	
}
