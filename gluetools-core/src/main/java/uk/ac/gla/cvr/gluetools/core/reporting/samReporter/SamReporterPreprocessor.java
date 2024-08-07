package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
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
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.MemberDistance;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.session.SamFileSession;
import uk.ac.gla.cvr.gluetools.core.session.SessionKey;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

public class SamReporterPreprocessor {

	/*
	 * Creates a set of temporary BAM files with the following properties:
	 * (a) reads are split arbitrarily over a number of files, equal to the number of CPUs specified for the GLUE SAM subsystem.
	 * (b) paired reads are always next to each other in the same file, with first read then second read.
	 * (c) first of pair / second of pair flags are always correct.
	 */
	public static SamReporterPreprocessorSession getPreprocessorSession(ConsoleCommandContext consoleCmdContext, String fileName, SamReporter samReporter) {
		SessionKey sessionKey = new SessionKey(SamFileSession.SESSION_TYPE, new String[] {samReporter.getModuleName(), fileName});
		SamFileSession currentSession = (SamFileSession) consoleCmdContext.getCurrentSession(sessionKey);
		if(currentSession != null) {
			return currentSession.getSamReporterPreprocessorSession();
		}
		return initPreprocessorSession(consoleCmdContext, fileName, samReporter);
	}

	public static SamReporterPreprocessorSession initPreprocessorSession(ConsoleCommandContext consoleCmdContext,
			String fileName, SamReporter samReporter) {
		ValidationStringency validationStringency = samReporter.getSamReaderValidationStringency();
		GlueLogger.getGlueLogger().finest("Preprocessing "+fileName+" into multiple BAM files");
		PropertiesConfiguration propertiesConfiguration = consoleCmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String tmpDirPath = propertiesConfiguration.getPropertyValue(SamUtils.SAM_TEMP_DIR_PROPERTY);
		int cpus = Integer.parseInt(propertiesConfiguration.getPropertyValue(SamUtils.SAM_NUMBER_CPUS, "4"));
		final SAMFileWriter[] bamWriters = new SAMFileWriter[cpus];
		SamReporterPreprocessorSession samReporterPreprocessorSession = new SamReporterPreprocessorSession(samReporter.getModuleName(), fileName);
		samReporterPreprocessorSession.preprocessedBamPaths = new String[cpus];
		
		DetailedReadLogger readLogger = new DetailedReadLogger();
		
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, fileName, validationStringency)) {
			SAMFileHeader header = samReader.getFileHeader().clone();
			header.setSortOrder(SortOrder.unsorted);
			SAMFileWriterFactory samFileWriterFactory = new SAMFileWriterFactory();
			for(int i = 0; i < cpus; i++) {
				String uuid = UUID.randomUUID().toString();
				File outputBamFile = new File(tmpDirPath, uuid+".bam");
				samReporterPreprocessorSession.preprocessedBamPaths[i] = outputBamFile.getAbsolutePath();
				bamWriters[i] = samFileWriterFactory.makeBAMWriter(header, true, outputBamFile);
			}
			Map<String, ReadPair> nameToPair = new LinkedHashMap<String, ReadPair>();
			
			SamUtils.iterateOverSamReader(samReader, samRecord -> {
				if(samRecord.getReadUnmappedFlag()) {
					return;
				}
				boolean firstOfPair = false;
				boolean secondOfPair = false;
				if(samRecord.getReadPairedFlag()) {
					firstOfPair = samRecord.getFirstOfPairFlag();
					secondOfPair = samRecord.getSecondOfPairFlag();
				}
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
			closeBamWriters(bamWriters, samReporterPreprocessorSession);

		} catch (IOException e) {
			closeBamWriters(bamWriters, samReporterPreprocessorSession);
			samReporterPreprocessorSession.cleanup();
			throw new RuntimeException(e);
		} finally {
		}
		return samReporterPreprocessorSession;
	}

	private static void closeBamWriters(final SAMFileWriter[] bamWriters,
			SamReporterPreprocessorSession samReporterPreprocessorSession) {
		for(int i = 0 ; i < bamWriters.length; i++) {
			SAMFileWriter bamWriter = bamWriters[i];
			String filePath = samReporterPreprocessorSession.preprocessedBamPaths[i];
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
	
	public static class SamReporterPreprocessorSession implements AutoCloseable {
		
		private String bamPath;
		
		private boolean storedInCmdContext = false;
		private String[] preprocessedBamPaths;
		private Map<ConsensusKey, DNASequence> cachedConsensus = new LinkedHashMap<ConsensusKey, DNASequence>();
		private Map<ConsensusKey, String> cachedTargetRefName = new LinkedHashMap<ConsensusKey, String>();
		private Map<SamToTargetSegsKey, List<QueryAlignedSegment>> cachedSamRefToTargetRefSegs = new LinkedHashMap<SamToTargetSegsKey, List<QueryAlignedSegment>>();
		private String samReporterName;
		
		public SamReporterPreprocessorSession(String samReporterName, String bamPath) {
			super();
			this.bamPath = bamPath;
			this.samReporterName = samReporterName;
		}

		public void setStoredInCmdContext(boolean storedInCmdContext) {
			this.storedInCmdContext = storedInCmdContext;
		}

		public String[] getPreprocessedBamPaths() {
			return preprocessedBamPaths;
		}
		
		public DNASequence getConsensus(ConsoleCommandContext consoleCmdContext, 
				SamReporter samReporter, ExtendedSamReporterCommand<?> samReporterCommand, boolean mayGenerateAmbiguities) {
			ConsensusKey consensusKey = new ConsensusKey(samReporter, samReporterCommand, mayGenerateAmbiguities);
			return getConsensus(consoleCmdContext, consensusKey);
		}

		private DNASequence getConsensus(ConsoleCommandContext consoleCmdContext, ConsensusKey consensusKey) {
			DNASequence consensusSequence = cachedConsensus.get(consensusKey);
			if(consensusSequence != null) {
				return consensusSequence;
			}
			SamReporter samReporter = Module.resolveModulePlugin(consoleCmdContext, SamReporter.class, samReporterName);
			consensusSequence = SamUtils.getSamConsensus(consoleCmdContext, bamPath, this, 
					samReporter.getSamReaderValidationStringency(), 
					consensusKey.suppliedSamRefName, consensusKey.minQScore, consensusKey.minMapQ, consensusKey.minDepth, 
					consensusKey.samRefSense, consensusKey.mayGenerateAmbiguities, 
					samReporter.getConsensusProduceAmbiguityCodes(), samReporter.getConsensusAmbiguityCodesMinDepth(), 
					samReporter.getConsensusAmbiguityMinProportion(), samReporter.getConsensusAmbiguityMinReads());
			cachedConsensus.put(consensusKey, consensusSequence);
			return consensusSequence;
		}
		
		public ReferenceSequence getTargetRefBasedOnPlacer(ConsoleCommandContext consoleCmdContext, SamReporter samReporter, ExtendedSamReporterCommand<?> samReporterCommand) {
			ConsensusKey consensusKey = new ConsensusKey(samReporter, samReporterCommand, false);
			String targetRefName = cachedTargetRefName.get(consensusKey);
			ReferenceSequence targetRef;
			if(targetRefName != null) {
				targetRef = GlueDataObject.lookup(consoleCmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));
				samReporter.log(Level.FINE, "Cached target reference found: "+targetRef.getName());
			} else {
				targetRef = getTargetRefDistance(consoleCmdContext, samReporter, consensusKey).getTargetRef();
			}
			return targetRef;
		}

		public ReferenceDistance getTargetRefDistanceBasedOnPlacer(ConsoleCommandContext consoleCmdContext, SamReporter samReporter, ExtendedSamReporterCommand<?> samReporterCommand) {
			ConsensusKey consensusKey = new ConsensusKey(samReporter, samReporterCommand, false);
			return getTargetRefDistance(consoleCmdContext, samReporter, consensusKey);
		}
		
		private ReferenceDistance getTargetRefDistance(ConsoleCommandContext consoleCmdContext, SamReporter samReporter,
				ConsensusKey consensusKey) {
			ReferenceSequence targetRef;
			DNASequence consensus = getConsensus(consoleCmdContext, consensusKey);
			MemberDistance memberDistance = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensus);
			AlignmentMember targetRefAlmtMember = memberDistance.getMember();
			targetRef = targetRefAlmtMember.targetReferenceFromMember();
			cachedTargetRefName.put(consensusKey, targetRef.getName());
			samReporter.log(Level.FINE, "Max likelihood placement of consensus sequence selected target reference "+targetRef.getName()+", distance: "+memberDistance.getDistance());
			return new ReferenceDistance(targetRef, memberDistance.getDistance());
		}
		
		public class ReferenceDistance {
			private ReferenceSequence targetRef;
			private Double distance;
			public ReferenceDistance(ReferenceSequence targetRef, Double distance) {
				super();
				this.targetRef = targetRef;
				this.distance = distance;
			}
			public ReferenceSequence getTargetRef() {
				return targetRef;
			}
			public Double getDistance() {
				return distance;
			}
		}
		
		
		public List<QueryAlignedSegment> getSamRefToTargetRefSegs(ConsoleCommandContext consoleCmdContext, SamReporter samReporter, ExtendedSamReporterCommand<?> samReporterCommand, String targetRefName) {
			ConsensusKey consensusKey = new ConsensusKey(samReporter, samReporterCommand, false);
			SamToTargetSegsKey samToTargetSegsKey = new SamToTargetSegsKey(consensusKey, targetRefName);
			List<QueryAlignedSegment> samRefToTargetRefSegs = cachedSamRefToTargetRefSegs.get(samToTargetSegsKey);
			if(samRefToTargetRefSegs != null) {
				return samRefToTargetRefSegs;
			}
			// auto-align consensus to target ref
			Aligner<?, ?> aligner = Aligner.getAligner(consoleCmdContext, samReporter.getAlignerModuleName());
			// compute consensus if necessary.
			DNASequence consensusSequence = getConsensus(consoleCmdContext, samReporter, samReporterCommand, false);
			Map<String, DNASequence> samConsensus = new LinkedHashMap<String, DNASequence>();
			samConsensus.put("samConsensus", consensusSequence);
			AlignerResult alignerResult = aligner.computeConstrained(consoleCmdContext, targetRefName, samConsensus);
			// extract segments from aligner result
			samRefToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get("samConsensus");
			cachedSamRefToTargetRefSegs.put(samToTargetSegsKey, samRefToTargetRefSegs);
			return samRefToTargetRefSegs;
			
		}

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
			if(!storedInCmdContext) {
				cleanup();
			}
		}
	}

	private static class ConsensusKey {
		public String suppliedSamRefName;
		private int minDepth;
		private int minQScore;
		private int minMapQ;
		private SamRefSense samRefSense;
		private boolean mayGenerateAmbiguities;
		
		public ConsensusKey(SamReporter samReporter, ExtendedSamReporterCommand<?> samReporterCommand, boolean mayGenerateAmbiguities) {
			super();
			this.minDepth = samReporterCommand.getConsensusMinDepth(samReporter);
			this.minQScore = samReporterCommand.getConsensusMinQScore(samReporter);
			this.minMapQ = samReporterCommand.getConsensusMinMapQ(samReporter);
			this.samRefSense = samReporterCommand.getSamRefSense(samReporter);
			this.suppliedSamRefName = samReporterCommand.getSuppliedSamRefName();
			this.mayGenerateAmbiguities = mayGenerateAmbiguities;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (mayGenerateAmbiguities ? 1231 : 1237);
			result = prime * result + minDepth;
			result = prime * result + minMapQ;
			result = prime * result + minQScore;
			result = prime * result + ((samRefSense == null) ? 0 : samRefSense.hashCode());
			result = prime * result + ((suppliedSamRefName == null) ? 0 : suppliedSamRefName.hashCode());
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
			ConsensusKey other = (ConsensusKey) obj;
			if (mayGenerateAmbiguities != other.mayGenerateAmbiguities)
				return false;
			if (minDepth != other.minDepth)
				return false;
			if (minMapQ != other.minMapQ)
				return false;
			if (minQScore != other.minQScore)
				return false;
			if (samRefSense != other.samRefSense)
				return false;
			if (suppliedSamRefName == null) {
				if (other.suppliedSamRefName != null)
					return false;
			} else if (!suppliedSamRefName.equals(other.suppliedSamRefName))
				return false;
			return true;
		}

		
		
		
		
	}

	private static class SamToTargetSegsKey {
		
		private ConsensusKey consensusKey;
		private String targetRefName;

		public SamToTargetSegsKey(ConsensusKey consensusKey, String targetRefName) {
			super();
			this.consensusKey = consensusKey;
			this.targetRefName = targetRefName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((consensusKey == null) ? 0 : consensusKey.hashCode());
			result = prime * result + ((targetRefName == null) ? 0 : targetRefName.hashCode());
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
			SamToTargetSegsKey other = (SamToTargetSegsKey) obj;
			if (consensusKey == null) {
				if (other.consensusKey != null)
					return false;
			} else if (!consensusKey.equals(other.consensusKey))
				return false;
			if (targetRefName == null) {
				if (other.targetRefName != null)
					return false;
			} else if (!targetRefName.equals(other.targetRefName))
				return false;
			return true;
		}
		
		
		
	}

	
}
