package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

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
		PropertiesConfiguration propertiesConfiguration = consoleCmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String tmpDirPath = propertiesConfiguration.getPropertyValue(SamUtils.SAM_TEMP_DIR_PROPERTY);
		int cpus = Integer.parseInt(propertiesConfiguration.getPropertyValue(SamUtils.SAM_NUMBER_CPUS, "4"));
		final SAMFileWriter[] bamWriters = new SAMFileWriter[cpus];
		SamFileSession samFileSession = new SamFileSession();
		samFileSession.preprocessedBamPaths = new String[cpus];
		
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, fileName, validationStringency)) {
			SAMFileHeader header = samReader.getFileHeader();
			SAMFileWriterFactory samFileWriterFactory = new SAMFileWriterFactory();
			for(int i = 0; i < cpus; i++) {
				String uuid = UUID.randomUUID().toString();
				File outputBamFile = new File(tmpDirPath, uuid+".bam");
				samFileSession.preprocessedBamPaths[i] = outputBamFile.getAbsolutePath();
				bamWriters[i] = samFileWriterFactory.makeBAMWriter(header, true, outputBamFile);
			}
			Map<String, ReadPair> nameToPair = new LinkedHashMap<String, ReadPair>();
			
			SamUtils.iterateOverSamReader(samReader, samRecord -> {
				boolean firstOfPair = samRecord.getFirstOfPairFlag();
				boolean secondOfPair = samRecord.getSecondOfPairFlag();
				if(firstOfPair || secondOfPair) {
					String readName = samRecord.getReadName();
					ReadPair readPair = nameToPair.remove(readName);
					if(readPair == null) {
						readPair = new ReadPair();
						if(firstOfPair) { 
							readPair.read1 = samRecord; 
						} else {
							readPair.read2 = samRecord; 
						}
						nameToPair.put(readName, readPair);
					} else {
						if(firstOfPair) { 
							writeRead(bamWriters, samRecord);
							writeRead(bamWriters, readPair.read2);
						} else {
							writeRead(bamWriters, readPair.read1);
							writeRead(bamWriters, samRecord);
						}
					}
				} else {
					// non paired read
					writeRead(bamWriters, samRecord);
				}
			});
			// now write out reads which claimed to be in a pair but aren't
			nameToPair.values().forEach(readPair -> {
				SAMRecord read1 = readPair.read1;
				if(read1 != null) {
					read1.setFirstOfPairFlag(false);
					writeRead(bamWriters, read1);
				}
				SAMRecord read2 = readPair.read2;
				if(read2 != null) {
					read1.setSecondOfPairFlag(false);
					writeRead(bamWriters, read2);
				}
			});

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
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
			samFileSession.cleanup();
		}
		return samFileSession;
	}

	// put the read in one of the writers, by arbitrarily selecting one based on the hash of the read name.
	private static void writeRead(SAMFileWriter[] writers, SAMRecord read) {
		int hashCode = read.getReadName().hashCode();
		int writerIndex = hashCode % writers.length;
		SAMFileWriter samFileWriter = writers[writerIndex];
		samFileWriter.addAlignment(read);
	}
	
	private static class ReadPair {
		SAMRecord read1;
		SAMRecord read2;
	}
	
	public static class SamFileSession {
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
	}

}
