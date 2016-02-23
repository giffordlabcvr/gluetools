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
import java.util.Optional;
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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class SamReporterCommand<R extends CommandResult> extends ModulePluginCommand<R, SamReporter> {

	
	public static final String AUTO_ALIGN = "autoAlign";
	public static final String SPECIFIC_MEMBER = "specificMember";
	public static final String FILE_NAME = "fileName";
	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String SAM_REF_NAME = "samRefName";
	public static final String REFERENCE_NAME = "refName";
	public static final String FEATURE_NAME = "featureName";
	public static final String MEMBER_SOURCE = "memberSource";
	public static final String MEMBER_SEQ_ID = "memberSeqId";
	
	protected static final String SAM_REPORTER_CMD_USAGE = 
			"-i <fileName> [-s <samRefName>] [-r <refName>] -f <featureName> [ (-a <alignmentName> | -m <alignmentName> <memberSource> <memberSeqId>) ]";

	protected static final String SAM_REPORTER_CMD_FURTHER_HELP = 
			"The input file <fileName> is loaded from the current load/save path. "+
		 	"It may be in SAM or BAM format.\n"+
		    "If <samRefName> is supplied, the processed reads are limited to those which are aligned to the "+
		    "named sequence in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
		    "file only names a single reference sequence.\n"+
			"The <refName> argument specifies a GLUE reference which must be the constraining reference of an "+
			"ancestor of the tip alignment (see below). The <refName> argument is only required if no "+
			"<defaultReferenceName> property is defined in the SamReporter module configuration. If both are supplied, "+
			"<refName> takes precedence.\n"+
			"The <featureName> argument specifies a feature location on the named reference. "+
		    "Together, <refName> and <featureName> specify the genome feature that will be scanned.\n"+
			"The SAM file reads can be placed in the GLUE alignment tree in 3 different ways:\n"+
		    "1. A specific alignment member can be specified on the command line (-m option). \n"+
		    "2. An alignment member is identified by transforming the SAM reference name. (neither option). In this case, the module must have a valid tipAlignmentMemberExtractorFormatter element."+
		    "3. A constrained alignment may be specified, the samReporter's aligner module aligns the consensus to the constraining reference of that alignment (-a option)\n"+
		    "If an alignment member is specified (1 or 2 above), this alignment member must be the same sequence as the named SAM reference. ";

	
	private String fileName;
	private String alignmentName;
	private String samRefName;
	private String referenceName;
	private String featureName;
	private String memberSource;
	private String memberSeqId;
	private boolean autoAlign;
	private boolean specificMember;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.samRefName = PluginUtils.configureStringProperty(configElem, SAM_REF_NAME, false);
		this.referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, false);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.autoAlign = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, AUTO_ALIGN, false)).orElse(false);
		this.specificMember = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, SPECIFIC_MEMBER, false)).orElse(false);

		this.alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, false);
		this.memberSource = PluginUtils.configureStringProperty(configElem, MEMBER_SOURCE, false);
		this.memberSeqId = PluginUtils.configureStringProperty(configElem, MEMBER_SEQ_ID, false);

		if(specificMember && autoAlign) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Incorrect to use both --specificMember and --autoAlign");
		}
		if(specificMember && 
				(alignmentName == null && memberSource == null && memberSeqId == null) ) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --specificMember is used, <alignmentName>, <memberSource> and <memberSeqId> should be defined");
		}
		if(autoAlign && 
				(alignmentName == null || memberSource != null && memberSeqId != null) ) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --autoAlign is used, only <alignmentName> should be defined");
		}
		if(!autoAlign && !specificMember && 
				(alignmentName != null || memberSource != null || memberSeqId != null) ) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "If neither --specificMember nor --autoAlign is used, <alignmentName>, <memberSource> and <memberSeqId> may not be defined");
		}
	}

	protected AlignmentMember getTipAlignmentMember(CommandContext cmdContext, SamReporter samReporter) {
		if(autoAlign) {
			return null;
		}
		if(specificMember) {
			return GlueDataObject.lookup(cmdContext, AlignmentMember.class, AlignmentMember.pkMap(alignmentName, memberSource, memberSeqId));
		}
		String samRefName;
 		try(SamReader samReader = newSamReader(cmdContext)) {
 			SAMSequenceRecord samReference = SamUtils.findReference(samReader, getFileName(), getSamRefName());
 			samRefName = samReference.getSequenceName();
 		} catch (IOException e) {
			throw new RuntimeException(e);
		}
 		String expressionString = samReporter.extractTipAlignmentMemberWhereClause(samRefName);
		if(expressionString == null) {
			throw new SamReporterCommandException(SamReporterCommandException.Code.TIP_ALIGNMENT_MEMBER_EXTRACTOR_FAILED, samRefName);
		}
		Expression expression = CayenneUtils.parseExpression(expressionString);
		List<AlignmentMember> members = GlueDataObject.query(cmdContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, expression));
		if(members.size() == 0) {
			throw new SamReporterCommandException(SamReporterCommandException.Code.TIP_ALIGNMENT_MEMBER_NOT_FOUND, samRefName, expressionString);
		}
		if(members.size() > 1) {
			throw new SamReporterCommandException(SamReporterCommandException.Code.AMBIGUOUS_TIP_ALIGNMENT_MEMBER_DEFINED);
		}
		return members.get(0);
	}
	
	protected Alignment getTipAlignment(CommandContext cmdContext, AlignmentMember tipAlignmentMember) {
		if(tipAlignmentMember == null) {
			return GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		} else {
			return tipAlignmentMember.getAlignment();
		}
	}


	protected FeatureLocation getScannedFeatureLoc(CommandContext cmdContext, SamReporter samReporter) {
		return GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getReferenceName(samReporter), featureName), false);
	}





	protected List<QueryAlignedSegment> getSamRefToGlueRefSegs(CommandContext cmdContext,
			SamReporter samReporter, AlignmentMember tipAlignmentMember, 
			ReferenceSequence tipAlmtRef, ReferenceSequence ancConstrainingRef) {
		List<QueryAlignedSegment> samRefToGlueRefSegs;
		if(tipAlignmentMember != null) {
			samRefToGlueRefSegs = tipAlignmentMember.getAlignedSegments().stream()
					.map(seg -> seg.asQueryAlignedSegment())
					.collect(Collectors.toList());
		} else {
			String samConsensusFastaID = "samConsensus";
			Map<String, DNASequence> samConsensusFastaMap = getSamConsensus(cmdContext, samReporter, samConsensusFastaID);
			Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, samReporter.getAlignerModuleName());
			AlignerResult alignerResult = aligner.doAlign(cmdContext, tipAlmtRef.getName(), samConsensusFastaMap);
			samRefToGlueRefSegs = alignerResult.getQueryIdToAlignedSegments().get(samConsensusFastaID);
		}
		Alignment tipAlignment = getTipAlignment(cmdContext, tipAlignmentMember);
		samRefToGlueRefSegs = tipAlignment.translateToAncConstrainingRef(cmdContext, samRefToGlueRefSegs, ancConstrainingRef);
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
			int samRefEnd = samRefStart + almtBlock.getLength()-1;
			int readStart = almtBlock.getReadStart();
			int readEnd = readStart + almtBlock.getLength()-1;
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
					if(referenceName == null) {
						ModuleMode moduleMode = (ModuleMode) cmdContext.peekCommandMode();
						String moduleName = moduleMode.getModuleName();
						Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName), true);
						SamReporter samReporter = (SamReporter) module.getModulePlugin(cmdContext.getGluetoolsEngine());
						String defaultReferenceName = samReporter.getDefaultReferenceName();
						if(defaultReferenceName != null) {
							referenceName = defaultReferenceName;
						}
					}
					if(referenceName != null) {
						ReferenceSequence referenceSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), true);
						if(referenceSequence != null) {
							return referenceSequence.getFeatureLocations().stream()
									.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
									.collect(Collectors.toList());
						}
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

	protected SamRecordFilter getSamRecordFilter(SamReader samReader, SamReporter samReporter) {
		return new SamReporterRecordFilter(samReader, samReporter);
	}
	
	private class SamReporterRecordFilter implements SamRecordFilter {

		private int samRefIndex;
		
		public SamReporterRecordFilter(SamReader samReader, SamReporter samReporter) {
			super();
			SAMSequenceRecord samReference = SamUtils.findReference(samReader, getFileName(), getSamRefName());
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

	protected String getReferenceName(SamReporter samReporter) {
		if(referenceName != null) {
			return referenceName;
		}
		String defaultReferenceName = samReporter.getDefaultReferenceName();
		if(defaultReferenceName != null) {
			return defaultReferenceName;
		}
		throw new SamReporterCommandException(SamReporterCommandException.Code.NO_GLUE_REFERENCE_DEFINED);
		
	}

	
}
