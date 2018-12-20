package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.AlignmentBlock;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamReporterPreprocessorSession;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtilsException.Code;

public class SamConsensusGenerator implements SamPairedParallelProcessor<SamConsensusGenerator.ConsensusContext, SamConsensusGenerator.ConsensusResult>{

	public String getNgsConsensus(ConsoleCommandContext cmdContext, SamReporterPreprocessorSession samReporterPreprocessorSession, ValidationStringency validationStringency, 
			String samRefName, int minQScore, int minMapQ, int minDepth, SamRefSense samRefSense) {
	
		if(!EnumSet.of(SamRefSense.FORWARD, SamRefSense.REVERSE_COMPLEMENT).contains(samRefSense)) {
			throw new RuntimeException("SAM ref sense should be determined by the point of forming the consensus");
		}
		
		Supplier<ConsensusContext> contextSupplier = () -> {
			ConsensusContext consensusContext = new ConsensusContext();
			consensusContext.samRefName = samRefName;
			consensusContext.minQScore = minQScore;
			consensusContext.samRefSense = samRefSense;
			return consensusContext;
		};
		ConsensusResult mergedResult = SamUtils.pairedParallelSamIterate(contextSupplier, cmdContext, samReporterPreprocessorSession, validationStringency, this);
		
	    StringBuffer consensus = new StringBuffer();
	    char[] bases = {'A', 'C', 'G', 'T'};
	    int[] counts = new int[4];
		char next;
		int best;
	    for(int i = 0; i < mergedResult.depth.length; i++) {
	    	next = 'N';
	    	if(mergedResult.depth[i] >= minDepth) {
	    		best = 0;
	    		counts[0] = mergedResult.aCounts[i];
	    		counts[1] = mergedResult.cCounts[i];
	    		counts[2] = mergedResult.gCounts[i];
	    		counts[3] = mergedResult.tCounts[i];
	    		for(int j = 0; j < 4; j++) {
	    			if(counts[j] > best) {
	    				next = bases[j];
	    				best = counts[j];
	    			} else if(counts[j] == best) {
	    				next = 'N';
	    			}
	    		}
	    	}
			consensus.append(next);
	    }
	    return consensus.toString();
	}

	public static class ConsensusContext {
		public SamRefSense samRefSense;
		private int minQScore;
		private int minMapQ;
		private int samReferenceIndex;
		private String samRefName; 
	    private int[] depth;
	    private int[] aCounts;
	    private int[] cCounts;
	    private int[] gCounts;
	    private int[] tCounts;
		public int samReferenceLength;
		
		public void recordBase(int samRefNt, char base) {
			switch(base) {
			case 'A':
				depth[samRefNt]++;
				aCounts[samRefNt]++;
				break;
			case 'C':
				depth[samRefNt]++;
				cCounts[samRefNt]++;
				break;
			case 'G':
				depth[samRefNt]++;
				gCounts[samRefNt]++;
				break;
			case 'T':
				depth[samRefNt]++;
				tCounts[samRefNt]++;
				break;
			case 'N':
				break;
			default:
				throw new SamUtilsException(Code.ALIGNMENT_LINE_USES_UNKNOWN_CHARACTER, Character.toString(base), Integer.toString((int) base));
			}
		}
		
	}

	public static class ConsensusResult {
	    private int[] depth;
	    private int[] aCounts;
	    private int[] cCounts;
	    private int[] gCounts;
	    private int[] tCounts;
	}

	@Override
	public void initContextForReader(ConsensusContext context, SamReader samReader) {

		SAMSequenceRecord samReference = samReader.getFileHeader().getSequenceDictionary().getSequence(context.samRefName);
		context.samReferenceLength = samReference.getSequenceLength();
		context.samReferenceIndex = samReference.getSequenceIndex();
		context.depth = new int[context.samReferenceLength];
		context.aCounts = new int[context.samReferenceLength];
		context.cCounts = new int[context.samReferenceLength];
		context.gCounts = new int[context.samReferenceLength];
		context.tCounts = new int[context.samReferenceLength];
		
	}

	@Override
	public void processPair(ConsensusContext context, SAMRecord record1, SAMRecord record2) {
    	if(record1.getReferenceIndex() != context.samReferenceIndex) {
    		processSingleton(context, record2);
    	} else if(record2.getReferenceIndex() != context.samReferenceIndex) {
    		processSingleton(context, record2);
    	} else {
    		TIntObjectMap<BaseWithQuality> samRefNtToBaseWithQuality1 = getSamRefNtToBaseWithQuality(context, record1);
    		TIntObjectMap<BaseWithQuality> samRefNtToBaseWithQuality2 = getSamRefNtToBaseWithQuality(context, record2);
    		int read1MapQ = record1.getMappingQuality();
    		int read2MapQ = record2.getMappingQuality();
			int readNameHashCoinFlip = Math.abs(record1.getReadName().hashCode()) % 2;

    		for(int samRefNt: samRefNtToBaseWithQuality1.keys()) {
    			BaseWithQuality baseWithQuality1 = samRefNtToBaseWithQuality1.get(samRefNt);
    			BaseWithQuality baseWithQuality2 = samRefNtToBaseWithQuality2.remove(samRefNt);
    			if(baseWithQuality2 == null) {
    				context.recordBase(samRefNt, baseWithQuality1.base);
    			} else {
    				int read1qual = baseWithQuality1.quality;
    				int read2qual = baseWithQuality2.quality;
    				if(read1qual < read2qual) {
    					context.recordBase(samRefNt, baseWithQuality2.base);
    				} else if(read1qual > read2qual) {
    					context.recordBase(samRefNt, baseWithQuality1.base);
    				} else if(read1MapQ != 255 && read2MapQ != 255 && read1MapQ < read2MapQ) {
    					context.recordBase(samRefNt, baseWithQuality2.base);
    				} else if(read1MapQ != 255 && read2MapQ != 255 && read1MapQ > read2MapQ) {
    					context.recordBase(samRefNt, baseWithQuality1.base);
    				} else if(readNameHashCoinFlip == 0) {
    					context.recordBase(samRefNt, baseWithQuality1.base);
    				} else {
    					context.recordBase(samRefNt, baseWithQuality2.base);
    				}
    			}
    		}
    		for(int samRefNt: samRefNtToBaseWithQuality2.keys()) {
				context.recordBase(samRefNt, samRefNtToBaseWithQuality2.get(samRefNt).base);
    		}
    	}
	}

	@Override
	public void processSingleton(ConsensusContext context, SAMRecord samRecord) {
    	if(samRecord.getReferenceIndex() != context.samReferenceIndex) {
    		return;
    	}
    	TIntObjectMap<BaseWithQuality> samRefNtToBaseWithQuality = getSamRefNtToBaseWithQuality(context, samRecord);
		for(int samRefNt: samRefNtToBaseWithQuality.keys()) {
			context.recordBase(samRefNt, samRefNtToBaseWithQuality.get(samRefNt).base);
		}
	}

	@Override
	public ConsensusResult contextResult(ConsensusContext context) {
		ConsensusResult consensusResult = new ConsensusResult();
		consensusResult.aCounts = context.aCounts;
		consensusResult.cCounts = context.cCounts;
		consensusResult.gCounts = context.gCounts;
		consensusResult.tCounts = context.tCounts;
		consensusResult.depth = context.depth;
		return consensusResult;
	}

	@Override
	public ConsensusResult reduceResults(ConsensusResult result1, ConsensusResult result2) {
		ConsensusResult consensusResult = new ConsensusResult();
		consensusResult.aCounts = new int[result1.aCounts.length];
		consensusResult.cCounts = new int[result1.cCounts.length];
		consensusResult.gCounts = new int[result1.gCounts.length];
		consensusResult.tCounts = new int[result1.tCounts.length];
		consensusResult.depth = new int[result1.depth.length];
		for(int i = 0; i < result1.depth.length; i++) {
			consensusResult.aCounts[i] = result1.aCounts[i]+result2.aCounts[i];
			consensusResult.cCounts[i] = result1.cCounts[i]+result2.cCounts[i];
			consensusResult.gCounts[i] = result1.gCounts[i]+result2.gCounts[i];
			consensusResult.tCounts[i] = result1.tCounts[i]+result2.tCounts[i];
			consensusResult.depth[i] = result1.depth[i]+result2.depth[i];
		}
		return consensusResult;
	}

	private TIntObjectMap<BaseWithQuality> getSamRefNtToBaseWithQuality(ConsensusContext context, SAMRecord samRecord) {
		TIntObjectMap<BaseWithQuality> samRefNtToBaseWithQuality = new TIntObjectHashMap<SamConsensusGenerator.BaseWithQuality>();

		if(samRecord.getMappingQuality() >= context.minMapQ) {

			String readString = samRecord.getReadString().toUpperCase();
			String qualityString = samRecord.getBaseQualityString();
			List<AlignmentBlock> alignmentBlocks = samRecord.getAlignmentBlocks();
			alignmentBlocks.forEach(alignmentBlock -> {
				int blockLength = alignmentBlock.getLength();
				int readStart = alignmentBlock.getReadStart();
				int refStart = alignmentBlock.getReferenceStart();

				for(int baseIndex = 0; baseIndex < blockLength; baseIndex++) {
					char readQualityChar = qualityString.charAt((readStart+baseIndex)-1);
					int qScore = SamUtils.qualityCharToQScore(readQualityChar);
					if(qScore < context.minQScore) {
						continue;
					}
					char readBase = Character.toUpperCase(readString.charAt((readStart+baseIndex)-1));
					char forwardSenseReadBase = SamUtils.getForwardSenseReadBase(context.samRefSense, readBase);
					if(forwardSenseReadBase == '=') {
						throw new SamUtilsException(SamUtilsException.Code.ALIGNMENT_LINE_USES_EQUALS);
					}
					int index = SamUtils.getForwardSenseSamRefIndex(context.samRefSense, context.samReferenceLength, refStart, baseIndex);
					switch(forwardSenseReadBase) {
					case 'A':
					case 'C':
					case 'G':
					case 'T':
					case 'N':
						samRefNtToBaseWithQuality.put(index, new BaseWithQuality(forwardSenseReadBase, qScore));
						break;
					default:
						throw new SamUtilsException(SamUtilsException.Code.ALIGNMENT_LINE_USES_UNKNOWN_CHARACTER, Character.toString(readBase), Integer.toString(readBase));
					}
				}
			});
		}
		
		return samRefNtToBaseWithQuality;
	}
	
	private class BaseWithQuality {
		char base;
		int quality;
		public BaseWithQuality(char base, int quality) {
			super();
			this.base = base;
			this.quality = quality;
		}
	}
	
	
}
