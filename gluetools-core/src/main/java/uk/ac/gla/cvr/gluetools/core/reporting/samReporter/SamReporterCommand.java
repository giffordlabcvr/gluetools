package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class SamReporterCommand<R extends CommandResult> extends ModulePluginCommand<R, SamReporter> {

	
	public static final String FILE_NAME = "fileName";
	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String SAM_REF_NAME = "samRefName";
	public static final String REFERENCE_NAME = "refName";
	public static final String FEATURE_NAME = "featureName";
	public static final String MEMBER_SOURCE = "memberSource";
	public static final String MEMBER_SEQ_ID = "memberSeqId";
	
	protected static final String SAM_REPORTER_CMD_USAGE = 
			"-i <fileName> [-s <samRefName>] -r <refName> -f <featureName> -a <alignmentName> [<memberSource> <memberSeqId>]";

	protected static final String SAM_REPORTER_CMD_FURTHER_HELP = 
			"The input file <fileName> is loaded from the current load/save path. "+
		 	"It may be in SAM or BAM format.\n"+
		    "If <samRefName> is supplied, the processed reads are limited to those which are aligned to the "+
		    "named sequence in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
		    "file only names a single reference sequence.\n"+
			"The <refName> parameter specifies a GLUE reference which must be the constraining reference of an "+
			"ancestor of the named tip alignment (see below).\n"+
			"The <featureName> parameter specifies a feature location on the named reference. "+
		    "Together, <refName> and <featureName> specify the genome feature that will be scanned.\n"+
		    "A constrained GLUE alignment <alignmentName> must be specified. "+
		    "This is typically a tip alignment matching the known clade of the input sequence. "+
		    "Optionally, <memberSource> and <memberSeqId> may be used to identify a member of the tip alignment. "+
		    "If so, this alignment member must be the same sequence as the named SAM reference. "+
		    "In this case the coordinate mapping of the alignment member will relate the input reads to "+
		    "reference <refName> in the alignment tree. "+
		    "If <memberSource> / <memberSeqId> are not specified, then instead, the consensus sequence of the input file "+
		    "will be aligned against the tip alignment's reference "+
		    "using the aligner module specified in the samReporter's config, to produce the required coordinate mapping.";

	
	private String fileName;
	private String alignmentName;
	private String samRefName;
	private String referenceName;
	private String featureName;
	private String memberSource;
	private String memberSeqId;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.samRefName = PluginUtils.configureStringProperty(configElem, SAM_REF_NAME, false);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		
		this.memberSource = PluginUtils.configureStringProperty(configElem, MEMBER_SOURCE, false);
		this.memberSeqId = PluginUtils.configureStringProperty(configElem, MEMBER_SEQ_ID, false);

		if(!(
				(memberSource == null && memberSeqId == null) || 
				(memberSource != null && memberSeqId != null) ) ) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <memberSource> and <memberSeqId> should be defined, or neither");
		}
		
		
	}

	protected Alignment getTipAlignment(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
	}


	protected FeatureLocation getScannedFeatureLoc(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(referenceName, featureName), false);
	}


	protected ReferenceSequence getScannedRef(CommandContext cmdContext, Alignment tipAlignment) {
		ReferenceSequence scannedReference = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), false);
		List<String> ancestorRefNames = tipAlignment.getAncestorReferences()
				.stream().map(ref -> ref.getName()).collect(Collectors.toList());
		
		if(!ancestorRefNames.contains(scannedReference.getName())) {
        	throw new SamReporterCommandException(SamReporterCommandException.Code.REFERENCE_DOES_NOT_CONSTRAIN_ANCESTOR, referenceName, alignmentName);
		}

		return scannedReference;
	}


	protected ReferenceSequence getConstrainingRef(Alignment alignment) {
		ReferenceSequence constrainingRef = alignment.getRefSequence();
		if(constrainingRef == null) {
        	throw new SamReporterCommandException(SamReporterCommandException.Code.ALIGNMENT_IS_UNCONSTRAINED, alignmentName);
		}
		return constrainingRef;
	}

	protected List<QueryAlignedSegment> getSamRefToGlueRefSegs(CommandContext cmdContext,
			SamReporter samReporter, Alignment tipAlignment,
			ReferenceSequence tipAlmtRef,
			ReferenceSequence scannedReference) {
		List<QueryAlignedSegment> samRefToGlueRefSegs;
		if(memberSource != null && memberSeqId != null) {
			AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class,
					AlignmentMember.pkMap(tipAlignment.getName(), memberSource, memberSeqId), false);
			samRefToGlueRefSegs = almtMember.getAlignedSegments().stream()
					.map(seg -> seg.asQueryAlignedSegment())
					.collect(Collectors.toList());
		} else {
			String samConsensusFastaID = "samConsensus";
			Map<String, DNASequence> samConsensusFastaMap = getSamConsensus(cmdContext, samReporter, samConsensusFastaID);
			Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, samReporter.getAlignerModuleName());
			AlignerResult alignerResult = aligner.doAlign(cmdContext, tipAlmtRef.getName(), samConsensusFastaMap);
			samRefToGlueRefSegs = alignerResult.getQueryIdToAlignedSegments().get(samConsensusFastaID);
		}
		Alignment currentAlignment = tipAlignment;
		// translate segments up the tree until we get to the scanned reference.
		while(!currentAlignment.getRefSequence().getName().equals(scannedReference.getName())) {
			Sequence refSeqSeq = currentAlignment.getRefSequence().getSequence();
			Alignment parentAlmt = currentAlignment.getParent();
			samRefToGlueRefSegs = parentAlmt.translateToRef(cmdContext, 
					refSeqSeq.getSource().getName(), refSeqSeq.getSequenceID(), samRefToGlueRefSegs);
			currentAlignment = parentAlmt;
		}
		samReporter.log("SAM reference to tip alignment reference mapping:");
		samReporter.log(samRefToGlueRefSegs.toString());
		return samRefToGlueRefSegs;
	}

	
	protected SamReader newSamReader(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		InputStream samInputStream = 
				ConsoleCommandContext.inputStreamFromFile(consoleCmdContext.fileStringToFile(fileName));
		return SamReaderFactory.makeDefault().open(SamInputResource.of(samInputStream));
	}
	
	private Map<String, DNASequence> getSamConsensus(CommandContext cmdContext, SamReporter samReporter, String fastaID) {
		Map<String, DNASequence> samConsensusFastaMap;
		try(SamReader samReader = newSamReader(cmdContext)) {

			SAMSequenceRecord samReference = SamUtils.findReference(samReader, fileName, samRefName);

			samReporter.log("Determining consensus sequence from NGS file "+fileName);
			samReporter.log("SAM reference: "+samReference.getSequenceName());
			String ngsConsensusFastaString = ">"+fastaID+"\n"+SamUtils.getNgsConsensus(samReader, samReference.getSequenceName());

			// samReporter.log("NGS consensus FASTA:\n"+ngsConsensusFastaString);

			samConsensusFastaMap = FastaUtils.parseFasta(ngsConsensusFastaString.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return samConsensusFastaMap;
	}

	protected String getSamRefName() {
		return samRefName;
	}
	
	protected String getFileName() {
		return fileName;
	}
	
	protected List<QueryAlignedSegment> getReadToSamRefSegs(SAMRecord samRecord) {
		List<QueryAlignedSegment> readToSamRefSegs = new ArrayList<QueryAlignedSegment>();
		samRecord.getAlignmentBlocks().forEach(almtBlock -> {
			int samRefStart = almtBlock.getReferenceStart();
			int samRefEnd = samRefStart+almtBlock.getLength()-1;
			int readStart = almtBlock.getReadStart();
			int readEnd = readStart+almtBlock.getLength()-1;
			readToSamRefSegs.add(new QueryAlignedSegment(samRefStart, samRefEnd, readStart, readEnd));
		});
		return readToSamRefSegs;
	}
	
	protected static class SamReporterCmdCompleter extends AdvancedCmdCompleter {
		public SamReporterCmdCompleter() {
			super();
			registerPathLookup("fileName", false);
			registerDataObjectNameLookup("refName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("refName");
					ReferenceSequence referenceSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), true);
					if(referenceSequence != null) {
						return referenceSequence.getFeatureLocations().stream()
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("memberSource", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String alignmentName = (String) bindings.get("alignmentName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), true);
					if(alignment != null) {
						return alignment.getMembers().stream()
								.map(mem -> new CompletionSuggestion(mem.getSequence().getSource().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerVariableInstantiator("memberSeqId", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String alignmentName = (String) bindings.get("alignmentName");
					String memberSource = (String) bindings.get("memberSource");
					Expression exp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignmentName);
					exp = exp.andExp(ExpressionFactory.matchExp(AlignmentMember.SOURCE_NAME_PATH, memberSource));
					List<AlignmentMember> members = GlueDataObject.query(cmdContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, exp));
					return members.stream()
							.map(mem -> new CompletionSuggestion(mem.getSequence().getSequenceID(), true))
							.collect(Collectors.toList());
				}
			});
		}
		
	}

	protected class IntHolder {
		int x;
	}

	protected SamRecordFilter getSamRecordFilter(SamReader samReader, SamReporter samReporter) {
		return new SamReporterRecordFilter(samReader, samReporter);
	}
	
	private class SamReporterRecordFilter implements SamRecordFilter {

		private int samRefIndex;
		private SamReporter samReporter;
		
		public SamReporterRecordFilter(SamReader samReader, SamReporter samReporter) {
			super();
			SAMSequenceRecord samReference = SamUtils.findReference(samReader, getFileName(), getSamRefName());
	        this.samRefIndex = samReference.getSequenceIndex();
	        this.samReporter = samReporter;
	        
		}

		@Override
		public boolean recordPasses(SAMRecord samRecord) {
			if(samRecord.getReferenceIndex() != samRefIndex) {
				return false;
			}
			return true;
		}
		
	}

	
}