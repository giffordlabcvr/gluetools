package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ShowReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.AddAlignedSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.RemoveAlignedSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.OriginalDataResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowOriginalDataCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

// could make this a module, but it seems quite fundamental to the model, 
// and doesn't seem to require much if any configuration.

@CommandClass(
		commandWords={"compute", "alignment"}, 
		description = "Align member segments using an aligner module", 
		docoptUsages = {"<alignmentName> <alignerModuleName> [-w <whereClause>]"}, 
		docoptOptions = {"-w <whereClause>, --whereClause <whereClause>  Qualify which members will be re-aligned"},
		metaTags={CmdMeta.updatesDatabase},
		furtherHelp = "Computes the aligned segments of certain members of the specified alignment, using a given aligner module. "+
		"If <whereClause> is not specified, all members of the alignment are re-aligned."+
		" Example: compute alignment AL1 blastAligner -w \"sequence.GENOTYPE = 4\""
)
public class ComputeAlignmentCommand extends ProjectModeCommand<ComputeAlignmentCommand.ComputeAlignmentResult> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	public static final String WHERE_CLAUSE = "whereClause";
	
	private String alignmentName;
	private String alignerModuleName;
	private Expression whereClause;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, true);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
	}
	

	@Override
	public ComputeAlignmentResult execute(CommandContext cmdContext) {
		// enter the alignment command mode to get the reference sequence name 
		String refName = getRefSequenceName(cmdContext);
		// enter the alignment command mode to get the member ID maps selected by the where clause
		List<Map<String, Object>> memberIDs = getMemberSequenceIdMaps(cmdContext);
		// get the original data for the reference sequence
		// modify the aligned segments and generate alignment results results for each selected member.
		List<Map<String, Object>> resultListOfMaps = 
				getAllAlignResults(cmdContext, memberIDs, refName);
		return new ComputeAlignmentResult(resultListOfMaps);
	}


	private <R extends AlignerResult, C extends Command<R>> List<Map<String, Object>> getAllAlignResults(
			CommandContext cmdContext, List<Map<String, Object>> memberIDs, String refName) {
		// get the align command's class for the module.
		Class<C> alignCommandClass = getAlignCommandClass(cmdContext);
		Map<String,String> queryIdToNucleotides = getMembersNtMap(cmdContext, memberIDs);
		R alignerResult = getAlignerResult(cmdContext, alignCommandClass, refName, queryIdToNucleotides);
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments = alignerResult.getQueryIdToAlignedSegments();
		for(Map<String, Object> memberIDmap: memberIDs) {
			String memberSourceName = (String) memberIDmap.get(AlignmentMember.SOURCE_NAME_PATH);
			String memberSeqId = (String) memberIDmap.get(AlignmentMember.SEQUENCE_ID_PATH);
			String memberFastaId = constructQueryId(memberSourceName, memberSeqId);
			List<QueryAlignedSegment> memberAlignedSegments = queryIdToAlignedSegments.get(memberFastaId);
			Map<String, Object> memberResultMap = applyMemberAlignedSegments(cmdContext, 
					memberSourceName, memberSeqId, memberAlignedSegments);
			resultListOfMaps.add(memberResultMap);
		}
		return resultListOfMaps;
	}

	private <R extends AlignerResult, C extends Command<R>> R getAlignerResult(
			CommandContext cmdContext, Class<C> alignCommandClass, 
			String refName, Map<String, String> queryIdToNucleotides) {
		R alignerResult;
		try(ModeCloser moduleMode = cmdContext.pushCommandMode("module", alignerModuleName)) {
			CommandBuilder<R, C> alignCmdBuilder = cmdContext.cmdBuilder(alignCommandClass)
				.set(AlignCommand.REFERENCE_NAME, refName);
			ArrayBuilder seqArrayBuilder = alignCmdBuilder
				.setArray(AlignCommand.SEQUENCE);
			queryIdToNucleotides.forEach((queryId, nts) ->
			{
				seqArrayBuilder.addObject()
					.set(AlignCommand.QUERY_ID, queryId)
					.set(AlignCommand.NUCLEOTIDES, nts);
			});
			alignerResult = alignCmdBuilder.execute();
		}
		return alignerResult;
	}
	
	
	private Map<String, String> getMembersNtMap(CommandContext cmdContext, List<Map<String, Object>> memberIDs) {
		Map<String, String> queryIdToNucleotides = new LinkedHashMap<String, String>();
		for(Map<String, Object> memberIDmap: memberIDs) {
			String memberSourceName = (String) memberIDmap.get(AlignmentMember.SOURCE_NAME_PATH);
			String memberSeqId = (String) memberIDmap.get(AlignmentMember.SEQUENCE_ID_PATH);
			OriginalDataResult memberSeqOriginalData = getOriginalData(cmdContext, memberSourceName, memberSeqId);
			SequenceFormat memberSeqFormat = memberSeqOriginalData.getFormat();
			byte[] base64Bytes = memberSeqOriginalData.getBase64Bytes();
			AbstractSequenceObject memberSeqObject = memberSeqFormat.sequenceObject();
			memberSeqObject.fromOriginalData(base64Bytes);
			String nucleotides = memberSeqObject.getNucleotides(cmdContext);
			String queryId = constructQueryId(memberSourceName, memberSeqId);
			queryIdToNucleotides.put(queryId, nucleotides);
		}
		return queryIdToNucleotides;
	}

	private String constructQueryId(String sourceName, String sequenceID) {
		return sourceName+"."+sequenceID;
	}
	
	private Map<String, Object> applyMemberAlignedSegments(
			CommandContext cmdContext,
			String memberSourceName,
			String memberSeqId,
			List<QueryAlignedSegment> memberAlignedSegments) {
		// enter the relevant alignment member mode, delete the existing aligned segments, and add new segments
		// according to the aligner result.
		int numRemovedSegments = 0;
		int numAddedSegments = 0;
		try(ModeCloser almtMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			try(ModeCloser memberMode = cmdContext.pushCommandMode("member", memberSourceName, memberSeqId)) {
				numRemovedSegments = cmdContext.cmdBuilder(RemoveAlignedSegmentCommand.class)
						.set(RemoveAlignedSegmentCommand.ALL_SEGMENTS, true)
						.execute().getNumber();
				for(IQueryAlignedSegment alignedSegment: memberAlignedSegments) {
					CreateResult addSegResult = cmdContext.cmdBuilder(AddAlignedSegmentCommand.class)
					.set(AddAlignedSegmentCommand.REF_START, alignedSegment.getRefStart())
					.set(AddAlignedSegmentCommand.REF_END, alignedSegment.getRefEnd())
					.set(AddAlignedSegmentCommand.MEMBER_START, alignedSegment.getQueryStart())
					.set(AddAlignedSegmentCommand.MEMBER_END, alignedSegment.getQueryEnd())
					.execute();
					numAddedSegments = numAddedSegments + addSegResult.getNumber();
				}
			}
		}
		return createMemberResultMap(memberSourceName, memberSeqId,
				numRemovedSegments, numAddedSegments);
	}


	private Map<String, Object> createMemberResultMap(String memberSourceName,
			String memberSeqId, int numRemovedSegments, int numAddedSegments) {
		Map<String, Object> memberResultMap = new LinkedHashMap<String, Object>();
		memberResultMap.put(AlignmentMember.SOURCE_NAME_PATH, memberSourceName);
		memberResultMap.put(AlignmentMember.SEQUENCE_ID_PATH, memberSeqId);
		memberResultMap.put(ComputeAlignmentResult.REMOVED_SEGMENTS, numRemovedSegments);
		memberResultMap.put(ComputeAlignmentResult.ADDED_SEGMENTS, numAddedSegments);
		return memberResultMap;
	}

	private OriginalDataResult getOriginalData(CommandContext cmdContext, String sourceName, String seqId) {
		// enter the sequence command mode to get the sequence original data.
		try (ModeCloser refSeqMode = cmdContext.pushCommandMode("sequence", sourceName, seqId)) {
			return cmdContext.cmdBuilder(ShowOriginalDataCommand.class).execute();
		}
	}



	private List<Map<String, Object>> getMemberSequenceIdMaps(CommandContext cmdContext) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			CommandBuilder<ListResult, ListMemberCommand> cmdBuilder = cmdContext.cmdBuilder(ListMemberCommand.class);
			if(whereClause != null) {
				cmdBuilder.set(ListMemberCommand.WHERE_CLAUSE, whereClause.toString());
			}
			cmdBuilder.setArray(ListMemberCommand.FIELD_NAME)
				.add(AlignmentMember.SOURCE_NAME_PATH)
				.add(AlignmentMember.SEQUENCE_ID_PATH);
			return cmdBuilder.execute().asListOfMaps();
		}
	}


	private String getRefSequenceName(CommandContext cmdContext) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			return cmdContext.cmdBuilder(ShowReferenceSequenceCommand.class).execute().getReferenceName();
		}
	}


	private <R extends AlignerResult, C extends Command<R>> Class<C> getAlignCommandClass(
			CommandContext cmdContext) {
		// Look up the module by name, check it is an aligner, and get its align command class.
		Module module = GlueDataObject.lookup(cmdContext.getObjectContext(), Module.class, Module.pkMap(alignerModuleName));
		ModulePlugin<?> modulePlugin = module.getModulePlugin(cmdContext.getGluetoolsEngine());
		if(!(modulePlugin instanceof Aligner<?, ?>)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Module "+alignerModuleName+" is not an aligner");
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Class<C> alignCommandClass = 
				((Aligner) modulePlugin).getAlignCommandClass();
		return alignCommandClass;
	}
	
	public static class ComputeAlignmentResult extends TableResult {
		public static final String REMOVED_SEGMENTS = "removedSegments";
		public static final String ADDED_SEGMENTS = "addedSegments";
		
		protected ComputeAlignmentResult(List<Map<String, Object>> listOfMaps) {
			super("computeAlignmentResult",  
					Arrays.asList(
							AlignmentMember.SOURCE_NAME_PATH, 
							AlignmentMember.SEQUENCE_ID_PATH, 
							REMOVED_SEGMENTS,
							ADDED_SEGMENTS),
					listOfMaps);
		}
	}
	
	
	@CompleterClass
	public static class Completer extends AlignmentNameCompleter {

		public Completer() {
			super();
			registerDataObjectNameLookup("alignerModuleName", Module.class, Module.NAME_PROPERTY);
		}

		
		
		
		
	}

	
}
