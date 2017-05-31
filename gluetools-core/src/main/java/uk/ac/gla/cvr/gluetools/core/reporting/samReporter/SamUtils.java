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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtilsException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class SamUtils {
	
	public static String getNgsConsensus(SamReader samReader, String samRefName) {

        SAMSequenceRecord samReference = samReader.getFileHeader().getSequenceDictionary().getSequence(samRefName);
        Integer samReferenceLength = samReference.getSequenceLength();
        int samReferenceIndex = samReference.getSequenceIndex();
        
        final int[] aCounts = new int[samReferenceLength];
        final int[] cCounts = new int[samReferenceLength];
        final int[] gCounts = new int[samReferenceLength];
        final int[] tCounts = new int[samReferenceLength];
        
        samReader.forEach(samRecord -> {
        	if(samRecord.getReferenceIndex() != samReferenceIndex) {
        		return;
        	}
        	String readString = samRecord.getReadString();
        	List<AlignmentBlock> alignmentBlocks = samRecord.getAlignmentBlocks();
        	alignmentBlocks.forEach(alignmentBlock -> {
        		int blockLength = alignmentBlock.getLength();
        		int readStart = alignmentBlock.getReadStart();
        		int refStart = alignmentBlock.getReferenceStart();
        		
        		
        		for(int baseIndex = 0; baseIndex < blockLength; baseIndex++) {
        			char readChar = Character.toUpperCase(readString.charAt((readStart+baseIndex)-1));
        			if(readChar == '=') {
        				throw new SamUtilsException(SamUtilsException.Code.ALIGNMENT_LINE_USES_EQUALS);
        			}
        			if(readChar == 'A') {
        				aCounts[refStart+baseIndex-1]++;
        			} else if(readChar == 'C') {
        				cCounts[refStart+baseIndex-1]++;
        			} else if(readChar == 'G') {
        				gCounts[refStart+baseIndex-1]++;
        			} else if(readChar == 'T') {
        				tCounts[refStart+baseIndex-1]++;
        			} else if(readChar == 'N') {
        				// ambiguity character
        			} else {
        				throw new SamUtilsException(SamUtilsException.Code.ALIGNMENT_LINE_USES_UNKNOWN_CHARACTER, Character.toString(readChar), Integer.toString(readChar));
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
			String fastaID) {
		Map<String, DNASequence> samConsensusFastaMap;
		try(SamReader samReader = newSamReader(cmdContext, fileName, validationStringency)) {

			SAMSequenceRecord samReference = findReference(samReader, fileName, samRefName);

			String ngsConsensusFastaString = ">"+fastaID+"\n"+SamUtils.getNgsConsensus(samReader, samReference.getSequenceName());

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
	
}
