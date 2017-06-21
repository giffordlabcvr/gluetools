package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.AlignmentBlock;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtilsException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class SamUtils {
	
	public static int getForwardSenseSamRefIndex(SamRefSense samRefSense, int samRefLength, int refStart, int baseIndex) {
		switch(samRefSense) {
		case FORWARD:
			return refStart+baseIndex-1;
		case REVERSE_COMPLEMENT:
			return samRefLength - (refStart+baseIndex);
		default:
			throw new RuntimeException("SAM ref sense should be determined by the point of querying a ref index");
		}
	}
	
	public static char getForwardSenseReadBase(SamRefSense samRefSense, char readBase) {
		switch(samRefSense) {
		case FORWARD:
			return readBase;
		case REVERSE_COMPLEMENT:
			return FastaUtils.complementChar(readBase);
		default:
			throw new RuntimeException("SAM ref sense should be determined by the point of using a read base");
		}
	}
	
	public static String getNgsConsensus(SamReader samReader, String samRefName, int minQScore, int minDepth, SamRefSense samRefSense) {

		if(!EnumSet.of(SamRefSense.FORWARD, SamRefSense.REVERSE_COMPLEMENT).contains(samRefSense)) {
			throw new RuntimeException("SAM ref sense should be determined by the point of forming the consensus");
		}
		
        SAMSequenceRecord samReference = samReader.getFileHeader().getSequenceDictionary().getSequence(samRefName);
        final int samReferenceLength = samReference.getSequenceLength();
        int samReferenceIndex = samReference.getSequenceIndex();
        
        final int[] depth = new int[samReferenceLength];
        final int[] aCounts = new int[samReferenceLength];
        final int[] cCounts = new int[samReferenceLength];
        final int[] gCounts = new int[samReferenceLength];
        final int[] tCounts = new int[samReferenceLength];
        
        samReader.forEach(samRecord -> {
        	if(samRecord.getReferenceIndex() != samReferenceIndex) {
        		return;
        	}
        	
			String readString = samRecord.getReadString().toUpperCase();
			String qualityString = samRecord.getBaseQualityString();
        	List<AlignmentBlock> alignmentBlocks = samRecord.getAlignmentBlocks();
        	alignmentBlocks.forEach(alignmentBlock -> {
        		int blockLength = alignmentBlock.getLength();
        		int readStart = alignmentBlock.getReadStart();
        		int refStart = alignmentBlock.getReferenceStart();
        		
        		
        		for(int baseIndex = 0; baseIndex < blockLength; baseIndex++) {
        			char readQualityChar = qualityString.charAt((readStart+baseIndex)-1);
        			if(SamUtils.qualityCharToQScore(readQualityChar) < minQScore) {
        				continue;
        			}
        			char readBase = Character.toUpperCase(readString.charAt((readStart+baseIndex)-1));
        			char forwardSenseReadBase = getForwardSenseReadBase(samRefSense, readBase);
        			if(forwardSenseReadBase == '=') {
        				throw new SamUtilsException(SamUtilsException.Code.ALIGNMENT_LINE_USES_EQUALS);
        			}
        			int index = getForwardSenseSamRefIndex(samRefSense, samReferenceLength, refStart, baseIndex);
					if(forwardSenseReadBase == 'A') {
						depth[index]++;
        				aCounts[index]++;
        			} else if(forwardSenseReadBase == 'C') {
						depth[index]++;
        				cCounts[index]++;
        			} else if(forwardSenseReadBase == 'G') {
						depth[index]++;
        				gCounts[index]++;
        			} else if(forwardSenseReadBase == 'T') {
						depth[index]++;
        				tCounts[index]++;
        			} else if(forwardSenseReadBase == 'N') {
        				// ambiguity character
        			} else {
        				throw new SamUtilsException(SamUtilsException.Code.ALIGNMENT_LINE_USES_UNKNOWN_CHARACTER, Character.toString(readBase), Integer.toString(readBase));
        			}
        		}
        	});
        });

        StringBuffer consensus = new StringBuffer();
        char[] bases = {'A', 'C', 'G', 'T'};
        int[] counts = new int[4];
		char next;
		int best;
        for(int i = 0; i < samReferenceLength; i++) {
        	next = 'N';
        	if(depth[i] >= minDepth) {
        		best = 0;
        		counts[0] = aCounts[i];
        		counts[1] = cCounts[i];
        		counts[2] = gCounts[i];
        		counts[3] = tCounts[i];
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
	
	
	public static SAMSequenceRecord findReference(SamReader samReader, String fileName, String samRefName) {
		SAMSequenceRecord samReference = null;
		List<SAMSequenceRecord> sequences = samReader.getFileHeader().getSequenceDictionary().getSequences();
        int numReferences = sequences.size();
        
        if(numReferences == 0) {
        	throw new SamUtilsException(SamUtilsException.Code.SAM_BAM_FILE_HAS_ZERO_REFERENCES, fileName);
        }


		if(samRefName == null) {
			if(numReferences > 1) {
	        	throw new SamUtilsException(SamUtilsException.Code.SAM_BAM_FILE_HAS_MULTIPLE_REFERENCES, fileName);
			} else {
				samReference = sequences.get(0);
			}
        } else {
        	for(SAMSequenceRecord foundSamRef: sequences) {
        		if(foundSamRef.getSequenceName().equals(samRefName)) {
        			samReference = foundSamRef;
        			break;
        		}
        	}
        	if(samReference == null) {
        		throw new SamUtilsException(SamUtilsException.Code.SAM_BAM_FILE_MISSING_REFERENCE, fileName, samRefName);
        	}
        }
		return samReference;
	}

	public static SamReader newSamReader(ConsoleCommandContext consoleCmdContext, String fileName, ValidationStringency validationStringency) {
		InputStream samInputStream = 
				ConsoleCommandContext.inputStreamFromFile(consoleCmdContext.fileStringToFile(fileName));
		SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault();
		if(validationStringency != null) {
			samReaderFactory.validationStringency(validationStringency);
		}
		return samReaderFactory.open(SamInputResource.of(samInputStream));
	}

	public static Map<String, DNASequence> getSamConsensus(ConsoleCommandContext cmdContext, String fileName, ValidationStringency validationStringency, String samRefName,
			String fastaID, int minQScore, int minDepth, SamRefSense samRefSense) {
		Map<String, DNASequence> samConsensusFastaMap;
		try(SamReader samReader = newSamReader(cmdContext, fileName, validationStringency)) {

			SAMSequenceRecord samReference = findReference(samReader, fileName, samRefName);

			String ngsConsensus = SamUtils.getNgsConsensus(samReader, samReference.getSequenceName(), minQScore, minDepth, samRefSense);
			if(ngsConsensus.replaceAll("N", "").isEmpty()) {
				throw new SamReporterCommandException(SamReporterCommandException.Code.NO_SAM_CONSENSUS, 
						Integer.toString(minQScore), Integer.toString(minDepth));
			}
			
			String ngsConsensusFastaString = ">"+fastaID+"\n"+
					ngsConsensus;

			samConsensusFastaMap = FastaUtils.parseFasta(ngsConsensusFastaString.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return samConsensusFastaMap;
	}

	public static class ReferenceBasedRecordFilter implements SamRecordFilter {

		private int samRefIndex;
		
		public ReferenceBasedRecordFilter(SamReader samReader, String fileName, String samRefName) {
			super();
			SAMSequenceRecord samReference = SamUtils.findReference(samReader, fileName, samRefName);
	        this.samRefIndex = samReference.getSequenceIndex();
		}

		@Override
		public boolean recordPasses(SAMRecord samRecord) {
			if(samRecord.getReferenceIndex() != samRefIndex) {
				return false;
			}
			return true;
		}
		
	}

	public static void iterateOverSamReader(SamReader samReader, Consumer<SAMRecord> recordConsumer) {
		try {
			samReader.forEach(recordConsumer);
		} catch(SAMFormatException sfe) {
			throw new SamUtilsException(sfe, Code.SAM_FORMAT_ERROR, sfe.getMessage());
		}
	}

	public static int qualityCharToQScore(char qualityChar) {
		return ((int) qualityChar) - 33;
	}
}
