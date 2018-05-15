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

import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamFileSession;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtilsException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class SamUtils {
	
	public static String 
		SAM_TEMP_DIR_PROPERTY = "gluetools.core.sam.temp.dir";

	public static String 
		SAM_NUMBER_CPUS = "gluetools.core.sam.cpus";

	
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

			SamConsensusGenerator samConsensusGenerator = new SamConsensusGenerator();
			
			String ngsConsensus = samConsensusGenerator.getNgsConsensus(cmdContext, fileName, validationStringency, 
					samReference.getSequenceName(), minQScore, minDepth, samRefSense);
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
	
	public static <M, R> R pairedParallelSamIterate(Supplier<M> contextSupplier, ConsoleCommandContext consoleCmdContext, 
			String samFileName, ValidationStringency validationStringency, 
			SamPairedParallelProcessor<M, R> samPairedParallelProcessor) {
		R reducedResult = null;
		List<SamReader> readers = new ArrayList<SamReader>();
		GlueLogger.getGlueLogger().finest("Preprocessing "+samFileName+" into multiple BAM files");
		SamFileSession samFileSession = SamReporterPreprocessor.preprocessSam(consoleCmdContext, samFileName, validationStringency);

		GlueLogger.getGlueLogger().finest("Running SamPairedParallelProcessor "+samPairedParallelProcessor.getClass().getSimpleName());
		List<M> contexts = new ArrayList<M>();
		List<PairedParallelSamWorker<M, R>> workers = new ArrayList<PairedParallelSamWorker<M, R>>();
		ReadLogger readLogger = new ReadLogger();
		for(int i = 0; i < samFileSession.preprocessedBamPaths.length; i++) {
			M context = contextSupplier.get();;
			contexts.add(context);
			SamReader samReader = 
					SamUtils.newSamReader(consoleCmdContext, samFileSession.preprocessedBamPaths[i], validationStringency);
			readers.add(samReader);
			samPairedParallelProcessor.initContextForReader(context, samReader);
			workers.add(new PairedParallelSamWorker<M, R>(context, samReader, samPairedParallelProcessor, readLogger));
		}
		List<R> results = new ArrayList<R>();

		try {

			ExecutorService samExecutorService = consoleCmdContext.getGluetoolsEngine().getSamExecutorService();
			List<Future<R>> futures = samExecutorService.invokeAll(workers);
			for(Future<R> future: futures) { // pick up results plus any exceptions.
				results.add(future.get()); 
			}
			readLogger.printMessage();
		} catch (Exception e) {
			throw new SamUtilsException(e, Code.SAM_PAIRED_READS_ERROR,  "Error during paired parallel SAM iteration: "+e.getLocalizedMessage());
		} finally {
			readers.forEach(reader -> { try {
				reader.close();
			} catch (Exception e) {
				GlueLogger.getGlueLogger().warning("Unable to close SamReader: "+e.getLocalizedMessage());
			} });
			samFileSession.cleanup();
		}
		
		reducedResult = results.get(0);
		for(int i = 1; i < results.size(); i++) {
			reducedResult = samPairedParallelProcessor.reduceResults(reducedResult, results.get(i));
		}
		
		return reducedResult;
		
	}
	

	public static int qualityCharToQScore(char qualityChar) {
		return ((int) qualityChar) - 33;
	}
	
	private static class PairedParallelSamWorker<M, R> implements Callable<R> {

		private M context;
		private SamReader samReader;
		private SamPairedParallelProcessor<M, R> samPairedParallelProcessor;
		private SAMRecord read1;
		private ReadLogger readLogger;
		
		public PairedParallelSamWorker(M context, SamReader samReader,
				SamPairedParallelProcessor<M, R> samPairedParallelProcessor, 
				ReadLogger readLogger) {
			super();
			this.context = context;
			this.samReader = samReader;
			this.samPairedParallelProcessor = samPairedParallelProcessor;
			this.readLogger = readLogger;
		}

		@Override
		public R call() throws Exception {
			SamUtils.iterateOverSamReader(samReader, samRecord -> {
				if(samRecord.getFirstOfPairFlag()) {
					if(read1 == null) {
						read1 = samRecord;
					} else {
						throw new SamUtilsException(Code.SAM_PAIRED_READS_ERROR, "Expected paired read "+read1.getReadName()+" 1/2 to be followed by 2/2");
					}
				} else if(samRecord.getSecondOfPairFlag()) {
					if(read1 == null) {
						throw new SamUtilsException(Code.SAM_PAIRED_READS_ERROR, "Expected paired read "+read1.getReadName()+" 2/2 to be preceded by 1/2");
					} else {
						if(samRecord.getReadName().equals(read1.getReadName())) {
							samPairedParallelProcessor.processPair(context, read1, samRecord);
							read1 = null;
							readLogger.logPair();
						} else {
							throw new SamUtilsException(Code.SAM_PAIRED_READS_ERROR, "Mispaired reads "+read1.getReadName()+" 1/2 with "+samRecord.getReadName()+" 2/2");
						}
					}
				} else {
					samPairedParallelProcessor.processSingleton(context, samRecord);
					readLogger.logSingleton();
				}
			});
			if(read1 != null) {
				throw new SamUtilsException(Code.SAM_PAIRED_READS_ERROR, "Expected paired read "+read1.getReadName()+" 1/2 to be followed by 2/2");
			}
			return samPairedParallelProcessor.contextResult(context);
		}
	}

	public static class ReadLogger {
		private static final int INTERVAL = 30000;
		private int pairs = 0;
		private int singletonReads = 0;
		private int totalReads = 0;
		
		public synchronized void logPair() {
			totalReads++;
			if(totalReads % INTERVAL == 0) {
				printMessage();
			}
			totalReads++;
			pairs++;
			if(totalReads % INTERVAL == 0) {
				printMessage();
			}
		}
		
		public synchronized void logSingleton() {
			singletonReads++;
			totalReads++;
			if(totalReads % INTERVAL == 0) {
				printMessage();
			}
		}
		
		public void printMessage() {
			GlueLogger.getGlueLogger().finest("Processed "+totalReads+" reads, ("+pairs+" pairs, "+singletonReads+" singletons)");
		}
		
	}
	
	
}





