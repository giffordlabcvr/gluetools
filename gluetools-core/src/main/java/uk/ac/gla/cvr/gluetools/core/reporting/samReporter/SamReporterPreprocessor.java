package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;




public class SamReporterPreprocessor {

	/*
	 * Creates a set of temporary BAM files with the following properties:
	 * (a) reads are split arbitrarily over a number of files, equal to the number of CPUs specified for the GLUE SAM subsystem.
	 * (b) paired reads are always next to each other in the same file, with first read then second read.
	 * (c) first of pair / second of pair flags are always correct.
	 */
	public static SamFileSession preprocessSam(ConsoleCommandContext consoleCmdContext, String fileName, ValidationStringency validationStringency) {
		GlueLogger.getGlueLogger().finest("Preprocessing "+fileName+" into multiple BAM files");
		PropertiesConfiguration propertiesConfiguration = consoleCmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String tmpDirPath = propertiesConfiguration.getPropertyValue(SamUtils.SAM_TEMP_DIR_PROPERTY);
		int cpus = Integer.parseInt(propertiesConfiguration.getPropertyValue(SamUtils.SAM_NUMBER_CPUS, "4"));
		final SAMFileWriter[] bamWriters = new SAMFileWriter[cpus];
		SamFileSession samFileSession = new SamFileSession();
		samFileSession.preprocessedBamPaths = new String[cpus];
		
		DetailedReadLogger readLogger = new DetailedReadLogger();
		
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, fileName, validationStringency)) {
			SAMFileHeader header = samReader.getFileHeader().clone();
			header.setSortOrder(SortOrder.unsorted);
			SAMFileWriterFactory samFileWriterFactory = new SAMFileWriterFactory();
			for(int i = 0; i < cpus; i++) {
				String uuid = UUID.randomUUID().toString();
				File outputBamFile = new File(tmpDirPath, uuid+".bam");
				samFileSession.preprocessedBamPaths[i] = outputBamFile.getAbsolutePath();
				bamWriters[i] = samFileWriterFactory.makeBAMWriter(header, true, outputBamFile);
			}
			Map<String, ReadPair> nameToPair = new LinkedHashMap<String, ReadPair>();
			
			SamUtils.iterateOverSamReader(samReader, samRecord -> {
				if(samRecord.getReadUnmappedFlag()) {
					return;
				}
				boolean firstOfPair = samRecord.getFirstOfPairFlag();
				boolean secondOfPair = samRecord.getSecondOfPairFlag();
				if(firstOfPair || secondOfPair) {
					String readName = samRecord.getReadName();
					ReadPair readPair = nameToPair.remove(readName);
					if(readPair == null) {
						readPair = new ReadPair();
						nameToPair.put(readName, readPair);
					}
					if(firstOfPair && !secondOfPair) {
						if(readPair.read2 != null) {
							// balanced -- 1 x firstOfPair, 1 x secondOfPair
							writeRead(bamWriters, samRecord);
							writeRead(bamWriters, readPair.read2);
							readLogger.logBalancedPair(!samRecord.getReadNegativeStrandFlag(), !readPair.read2.getReadNegativeStrandFlag());
						} else if(readPair.read1 == null) {
							readPair.read1 = samRecord; 
						} else {
							// unbalanced -- 2 x firstOfPair
							samRecord.setSecondOfPairFlag(true); // fix samRecord
							samRecord.setFirstOfPairFlag(false);
							writeRead(bamWriters, readPair.read1);
							writeRead(bamWriters, samRecord);
							readLogger.logUnbalancedPair(!readPair.read1.getReadNegativeStrandFlag(), !samRecord.getReadNegativeStrandFlag());
						}
					} else if(!firstOfPair && secondOfPair) {
						if(readPair.read1 != null) {
							// balanced -- 1 x firstOfPair, 1 x secondOfPair
							writeRead(bamWriters, readPair.read1);
							writeRead(bamWriters, samRecord);
							readLogger.logBalancedPair(!readPair.read1.getReadNegativeStrandFlag(), !samRecord.getReadNegativeStrandFlag());
						} else if(readPair.read2 == null) {
							readPair.read2 = samRecord; 
						} else {
							// unbalanced -- 2 x secondOfPair
							samRecord.setFirstOfPairFlag(true); // fix samRecord
							samRecord.setSecondOfPairFlag(false); 
							writeRead(bamWriters, samRecord);
							writeRead(bamWriters, readPair.read2);
							readLogger.logUnbalancedPair(!samRecord.getReadNegativeStrandFlag(), !readPair.read2.getReadNegativeStrandFlag());
						}
					} else {
						// unbalanced -- some other combination.
						if(readPair.read1 == null) {
							readPair.read1 = samRecord; 
						} else {
							readPair.read1.setFirstOfPairFlag(true); // fix read1
							readPair.read1.setSecondOfPairFlag(false); 
							samRecord.setSecondOfPairFlag(true); // fix samRecord
							samRecord.setFirstOfPairFlag(false);
							writeRead(bamWriters, readPair.read1);
							writeRead(bamWriters, samRecord);
							readLogger.logUnbalancedPair(!readPair.read1.getReadNegativeStrandFlag(), !samRecord.getReadNegativeStrandFlag());
						}						
					}
				} else {
					// non paired read
					writeRead(bamWriters, samRecord);
					readLogger.logSingleton();
				}
			});
			// now write out reads which claimed to be in a pair but aren't
			// or where the mate is unmapped.
			nameToPair.values().forEach(readPair -> {
				SAMRecord read1 = readPair.read1;
				if(read1 != null) {
					read1.setFirstOfPairFlag(false);
					writeRead(bamWriters, read1);
					readLogger.logSingleton();
				}
				SAMRecord read2 = readPair.read2;
				if(read2 != null) {
					read2.setSecondOfPairFlag(false);
					writeRead(bamWriters, read2);
					readLogger.logSingleton();
				}
			});
			readLogger.printMessage();
			closeBamWriters(bamWriters, samFileSession);

		} catch (IOException e) {
			closeBamWriters(bamWriters, samFileSession);
			samFileSession.cleanup();
			throw new RuntimeException(e);
		} finally {
		}
		return samFileSession;
	}

	private static void closeBamWriters(final SAMFileWriter[] bamWriters,
			SamFileSession samFileSession) {
		for(int i = 0 ; i < bamWriters.length; i++) {
			SAMFileWriter bamWriter = bamWriters[i];
			String filePath = samFileSession.preprocessedBamPaths[i];
			if(bamWriter != null) {
				try {
					bamWriter.close();
				} catch(Exception ee) {
					GlueLogger.getGlueLogger().log(Level.WARNING, "Unable to close SamFileWriter for file "+filePath+": "+ee.getLocalizedMessage());
				};
			}
		}
	}

	// put the read in one of the writers, by arbitrarily selecting one based on the hash of the read name.
	private static void writeRead(SAMFileWriter[] writers, SAMRecord read) {
		int hashCode = read.getReadName().hashCode();
		int writerIndex = Math.abs(hashCode) % writers.length;
		SAMFileWriter samFileWriter = writers[writerIndex];
		samFileWriter.addAlignment(read);
	}
	
	private static class ReadPair {
		SAMRecord read1;
		SAMRecord read2;
	}
	
	public static class SamFileSession implements AutoCloseable {
		String[] preprocessedBamPaths;

		public void cleanup() {
			for(int i = 0 ; i < preprocessedBamPaths.length; i++) {
				String filePath = preprocessedBamPaths[i];
				if(filePath != null) {
					File file = new File(filePath);
					if(file.exists()) {
						try {
							file.delete();
						} catch(Exception ee) {
							GlueLogger.getGlueLogger().log(Level.WARNING, "Unable to delete file "+filePath+": "+ee.getLocalizedMessage());
						}
					}
				}
			}
		}

		@Override
		public void close() {
			cleanup();
		}
	}

}
