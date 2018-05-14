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
import htsjdk.samtools.SAMFlag;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
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
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.RecordsCounter;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate amino acids in a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] -r <acRefName> -f <featureName> (-p | [-l] [-t <targetRefName>] [-a <tipAlmtName>]) [-q <minQScore>] [-e <minDepth>] [-P <minAAPct>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-n <samRefSense>, --samRefSense <samRefSense>        SAM ref seq sense",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-p, --maxLikelihoodPlacer                            Use ML placer module",
				"-l, --autoAlign                                      Auto-align consensus",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target GLUE reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
				"-q <minQScore>, --minQScore <minQScore>              Minimum Phred quality score",
				"-e <minDepth>, --minDepth <minDepth>                 Minimum depth",
				"-P <minAAPct>, --minAAPct <minAAPct>                 Minimum AA percentage",

		},
		furtherHelp = 
			"This command translates a SAM/BAM file reads to amino acids. "+
			"If <samRefName> is supplied, the translated reads are limited to those which are aligned to the "+
			"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
			"file only names a single reference sequence.\n"+
			"The translation is based on a 'target' GLUE reference sequence's place in the alignment tree. "+
			"The <samRefSense> may be FORWARD or REVERSE_COMPLEMENT, indicating the presumed sense of the SAM reference, relative to the GLUE references."+
			"If the --maxLikelihoodPlacer option is used, an ML placement is performed, and the target reference is "+
			"identified as the closest according to this placement. "+
			"The target reference may alternatively be specified using <targetRefName>."+
			"Or, inferred from the SAM reference name, if <targetRefName> is not supplied and the module is appropriately configured. "+
			"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
			"is the reference sequence  mentioned in the SAM file. "+
			"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
			"consensus and the target GLUE reference. \n"+
			"The --autoAlign option is implicit if --maxLikelihoodPlacer is used. "+
			"The target reference sequence must be a member of a constrained "+
			"'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
			"inferred from the target reference if possible. "+
			"The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
			"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
			"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
			"The translated amino acids will be limited to the specified feature location.\n"+
			"Reads will not contribute to the translation at a given codon location if any reported nucleotide quality score at that location is less than "+
			"<minQScore> (default value is derived from the module config). \n"+
			"No result will be generated for a codon location if the number of contributing reads is less than <minDepth> "+
			"(default value is derived from the module config).\n"+
			"Amino acid values will only display in the result if the percentage of reads contributing that value is at least <minAAPct> (default 0).",

		metaTags = {CmdMeta.consoleOnly}	
)
public class SamAminoAcidCommand extends AlignmentTreeSamReporterCommand<SamAminoAcidResult> 
	implements ProvidedProjectModeCommand{


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

		DNASequence consensusSequence = null;
		ReferenceSequence targetRef;
		AlignmentMember tipAlmtMember;
		if(useMaxLikelihoodPlacer()) {
			Map<String, DNASequence> consensusMap = SamUtils.getSamConsensus(consoleCmdContext, getFileName(), 
					samReporter.getSamReaderValidationStringency(), getSuppliedSamRefName(),"samConsensus", getMinQScore(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
			consensusSequence = consensusMap.get("samConsensus");
			tipAlmtMember = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensusSequence);
			targetRef = tipAlmtMember.targetReferenceFromMember();
			samReporter.log(Level.FINE, "Max likelihood placement of consensus sequence selected target reference "+targetRef.getName());
		} else {
			targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
					ReferenceSequence.pkMap(establishTargetRefName(consoleCmdContext, samReporter, samRefInfo.getSamRefName(), consensusSequence)));
			tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName(consoleCmdContext, samReporter, samRefInfo.getSamRefName()));
		}
		
		Alignment tipAlmt = tipAlmtMember.getAlignment();
		ReferenceSequence ancConstrainingRef = tipAlmt.getAncConstrainingRef(cmdContext, getAcRefName());

		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getAcRefName(), getFeatureName()), false);
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();

		List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, consoleCmdContext, targetRef, consensusSequence);
		
		// translate segments to tip alignment reference
		List<QueryAlignedSegment> samRefToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
				tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
				samRefToTargetRefSegs);
		
		// translate segments to ancestor constraining reference
		List<QueryAlignedSegment> samRefToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, samRefToTipAlmtRefSegs, ancConstrainingRef);

		// trim down to the feature area.
		List<ReferenceSegment> featureRefSegs = featureLoc.getSegments().stream()
				.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
		List<QueryAlignedSegment> samRefToAncConstrRefSegs = 
					ReferenceSegment.intersection(samRefToAncConstrRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			
		// truncate to codon aligned
		Integer codon1Start = featureLoc.getCodon1Start(cmdContext);

		List<QueryAlignedSegment> samRefToAncConstrRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, samRefToAncConstrRefSegs);

		if(samRefToAncConstrRefSegsCodonAligned.isEmpty()) {
			return new SamAminoAcidResult(Collections.emptyList());
		}
		
		SamRefSense samRefSense = getSamRefSense(samReporter);

		TIntObjectMap<LabeledCodon> ancConstrRefNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);

		// build a map from anc constr ref NT to AA read count.
		TIntObjectMap<AminoAcidReadCount> ancConstrRefNtToAminoAcidReadCount = new TIntObjectHashMap<AminoAcidReadCount>();
		List<Integer> mappedAncConstrRefNts = new ArrayList<Integer>();
		for(QueryAlignedSegment qaSeg: samRefToAncConstrRefSegsCodonAligned) {
			for(int ancConstrRefNt = qaSeg.getRefStart(); ancConstrRefNt <= qaSeg.getRefEnd(); ancConstrRefNt++) {
				if(TranslationUtils.isAtStartOfCodon(codon1Start, ancConstrRefNt)) {
					mappedAncConstrRefNts.add(ancConstrRefNt);
					LabeledCodon labeledCodon = ancConstrRefNtToLabeledCodon.get(ancConstrRefNt);
					int samRefNt = ancConstrRefNt + qaSeg.getReferenceToQueryOffset();
					int resultSamRefNt = samRefNt;
	        		if(samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
	        			// we want to report results in the SAM file's own coordinates.
	        			resultSamRefNt = ReferenceSegment.reverseLocationSense(samRefInfo.getSamRefLength(), samRefNt);
	        		}

					ancConstrRefNtToAminoAcidReadCount.put(ancConstrRefNt, new AminoAcidReadCount(labeledCodon, resultSamRefNt));
				}
			}
		}
		
		// translate reads.
		final Translator translator = new CommandContextTranslator(cmdContext);
		
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
				samReporter.getSamReaderValidationStringency())) {
			
			SamRecordFilter samRecordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, getFileName(), getSuppliedSamRefName());

	        final RecordsCounter recordsCounter = samReporter.new RecordsCounter();
			
			SamUtils.iterateOverSamReader(samReader, samRecord -> {
				if(!samRecordFilter.recordPasses(samRecord)) {
					return;
				}
				
				List<QueryAlignedSegment> readToSamRefSegs = samReporter.getReadToSamRefSegs(samRecord);
				String readString = samRecord.getReadString().toUpperCase();
				String qualityString = samRecord.getBaseQualityString();
        		if(samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
        			readToSamRefSegs = QueryAlignedSegment.reverseSense(readToSamRefSegs, readString.length(), samRefInfo.getSamRefLength());
        			readString = FastaUtils.reverseComplement(readString);
        			qualityString = StringUtils.reverseString(qualityString);
        		}

        		
        		
				List<QueryAlignedSegment> readToAncConstrRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToAncConstrRefSegs);
				
				
				List<QueryAlignedSegment> readToAncConstrRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, readToAncConstrRefSegs);

				List<QueryAlignedSegment> readToAncConstrRefSegsFiltered = filterByQuality(readToAncConstrRefSegsCodonAligned, qualityString, getMinQScore(samReporter)); 
				
				for(QueryAlignedSegment readToAncConstRefSeg: readToAncConstrRefSegsFiltered) {
					CharSequence nts = SegmentUtils.base1SubString(readString, readToAncConstRefSeg.getQueryStart(), readToAncConstRefSeg.getQueryEnd());
					String segAAs = translator.translateToAaString(nts);
					Integer ancConstrRefNt = readToAncConstRefSeg.getRefStart();
					for(int i = 0; i < segAAs.length(); i++) {
						char segAA = segAAs.charAt(i);
						AminoAcidReadCount aminoAcidReadCount = ancConstrRefNtToAminoAcidReadCount.get(ancConstrRefNt);
						aminoAcidReadCount.addAaRead(segAA);
						ancConstrRefNt += 3;
					}
				}
				recordsCounter.processedRecord();
				recordsCounter.logRecordsProcessed();
			});
			recordsCounter.logTotalRecordsProcessed();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		final List<LabeledAminoAcidReadCount> rowData = new ArrayList<LabeledAminoAcidReadCount>();
		
		for(Integer ancConstrRefNt: mappedAncConstrRefNts) {
			AminoAcidReadCount aminoAcidReadCount = ancConstrRefNtToAminoAcidReadCount.get(ancConstrRefNt);
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

	private List<QueryAlignedSegment> filterByQuality(
			List<QueryAlignedSegment> readToAncConstrRefSegsCodonAligned,
			String qualityString, int minQScore) {
		List<ReferenceSegment> poorQualityCodons = new ArrayList<ReferenceSegment>();
		for(QueryAlignedSegment readToAncConstrRefSeg: readToAncConstrRefSegsCodonAligned) {
			for(int r = readToAncConstrRefSeg.getRefStart(); r <= readToAncConstrRefSeg.getRefEnd(); r += 3) {
				int q = r+readToAncConstrRefSeg.getReferenceToQueryOffset();
				boolean poorQuality = false;
				char qualityChar1 = SegmentUtils.base1Char(qualityString, q);
				if(SamUtils.qualityCharToQScore(qualityChar1) < minQScore) {
					poorQuality = true;
				} else {
					if(q+1 <= readToAncConstrRefSeg.getQueryEnd()) {
						char qualityChar2 = SegmentUtils.base1Char(qualityString, q+1);
						if(SamUtils.qualityCharToQScore(qualityChar2) < minQScore) {
							poorQuality = true;
						}
					} else {
						if(q+2 <= readToAncConstrRefSeg.getQueryEnd()) {
							char qualityChar3 = SegmentUtils.base1Char(qualityString, q+2);
							if(SamUtils.qualityCharToQScore(qualityChar3) < minQScore) {
								poorQuality = true;
							}
						}
					}
				}
				if(poorQuality) {
					poorQualityCodons.add(new ReferenceSegment(r, r+2));
				}
			}
		}
		return ReferenceSegment.subtract(readToAncConstrRefSegsCodonAligned, poorQualityCodons);
	}

	private class AminoAcidReadCount {
		private LabeledCodon labeledCodon;
		private int samRefNt;
		
		public AminoAcidReadCount(LabeledCodon labeledCodon, int samRefNt) {
			super();
			this.labeledCodon = labeledCodon;
			this.samRefNt = samRefNt;
		}
		private int totalReadsAtCodon = 0;
		TCharIntMap aaToReadCount = new TCharIntHashMap();
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
		}
	}




	
}
