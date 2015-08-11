package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ShowReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.AddAlignedSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.RemoveAlignedSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ShowSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ShowSequenceResult;
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
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

// could make this a module, but it seems quite fundamental to the model, 
// and doesn't seem to require much if any configuration.

@CommandClass(
		commandWords={"compute", "alignment"}, 
		description = "Align member segments using an aligner module", 
		docoptUsages = {"<alignmentName> <alignerModuleName> [-w <whereClause>]"}, 
		docoptOptions = {"-w <whereClause>, --whereClause <whereClause>  Qualify which members will be re-aligned"},
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
		OriginalDataResult refSeqOriginalData = getReferenceSeqOriginalData(cmdContext, refName);
		String refSeqFormatString = refSeqOriginalData.getFormatString();
		String refSeqBase64String = refSeqOriginalData.getBase64String();

		// modify the aligned segments and generate alignment results results for each selected member.
		List<Map<String, Object>> resultListOfMaps = getAllAlignResults(
				cmdContext, memberIDs, refSeqFormatString, refSeqBase64String);
		return new ComputeAlignmentResult(resultListOfMaps);
	}


	private <R extends AlignerResult, C extends Command<R>> List<Map<String, Object>> getAllAlignResults(
			CommandContext cmdContext, List<Map<String, Object>> memberIDs,
			String refSeqFormatString, String refSeqBase64String) {
		// get the align command's class for the module.
		Class<C> alignCommandClass = getAlignCommandClass(cmdContext);
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		for(Map<String, Object> memberIDmap: memberIDs) {
			Map<String, Object> memberResultMap = getMemberAlignResults(
					cmdContext, refSeqFormatString, refSeqBase64String, alignCommandClass, memberIDmap);
			resultListOfMaps.add(memberResultMap);
		}
		return resultListOfMaps;
	}


	private <R extends AlignerResult, C extends Command<R>> Map<String, Object> getMemberAlignResults(
			CommandContext cmdContext,
			String refSeqFormatString,
			String refSeqBase64String,
			Class<C> alignCommandClass,
			Map<String, Object> memberIDmap) {
		// enter the relevant sequence mode to get the member sequence original data
		String memberSourceName = (String) memberIDmap.get(AlignmentMember.SOURCE_NAME_PATH);
		String memberSeqId = (String) memberIDmap.get(AlignmentMember.SEQUENCE_ID_PATH);
		OriginalDataResult memberSeqOriginalData = getOriginalData(cmdContext, memberSourceName, memberSeqId);
		String memberSeqFormatString = memberSeqOriginalData.getFormatString();
		String memberSeqBase64String = memberSeqOriginalData.getBase64String();
		// run the aligner on the reference and member sequence original data.
		R alignerResult;
		try(ModeCloser moduleMode = cmdContext.pushCommandMode("module", alignerModuleName)) {
			alignerResult = cmdContext.cmdBuilder(alignCommandClass)
				.set(AlignCommand.REFERENCE_FORMAT, refSeqFormatString)
				.set(AlignCommand.REFERENCE_BASE64, refSeqBase64String)
				.set(AlignCommand.QUERY_FORMAT, memberSeqFormatString)
				.set(AlignCommand.QUERY_BASE64, memberSeqBase64String)
				.execute();
		}
		// enter the relevant alignment member mode, delete the existing aligned segments, and add new segments
		// according to the aligner result.
		int numRemovedSegments = 0;
		int numAddedSegments = 0;
		try(ModeCloser almtMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			try(ModeCloser memberMode = cmdContext.pushCommandMode("member", memberSourceName, memberSeqId)) {
				numRemovedSegments = cmdContext.cmdBuilder(RemoveAlignedSegmentCommand.class)
						.set(RemoveAlignedSegmentCommand.ALL_SEGMENTS, true)
						.execute().getNumber();
				for(AlignerResult.AlignedSegment alignedSegment: alignerResult.getAlignedSegments()) {
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


	private OriginalDataResult getReferenceSeqOriginalData(
			CommandContext cmdContext, String refName) {
		// enter the reference command mode to get the reference sourceName and sequence ID.
		ShowSequenceResult showSequenceResult = getReferenceSequenceResult(cmdContext, refName);
		return getOriginalData(cmdContext, showSequenceResult.getSourceName(), showSequenceResult.getSequenceID());
	}


	private OriginalDataResult getOriginalData(CommandContext cmdContext, String sourceName, String seqId) {
		// enter the sequence command mode to get the sequence original data.
		try (ModeCloser refSeqMode = cmdContext.pushCommandMode("sequence", sourceName, seqId)) {
			return cmdContext.cmdBuilder(ShowOriginalDataCommand.class).execute();
		}
	}


	private ShowSequenceResult getReferenceSequenceResult(CommandContext cmdContext, String refName) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("reference", refName)) {
			return cmdContext.cmdBuilder(ShowSequenceCommand.class).execute();
		}
	}


	private List<Map<String, Object>> getMemberSequenceIdMaps(CommandContext cmdContext) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			CommandBuilder<ListResult, ListMemberCommand> cmdBuilder = cmdContext.cmdBuilder(ListMemberCommand.class);
			if(whereClause != null) {
				cmdBuilder.set(ListMemberCommand.WHERE_CLAUSE, whereClause.toString());
			}
			return cmdBuilder.setArray(ListMemberCommand.FIELD_NAME)
				.add(AlignmentMember.SOURCE_NAME_PATH)
				.add(AlignmentMember.SEQUENCE_ID_PATH)
				.execute().asListOfMaps();
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
			super("updateAlignmentResult",  
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

		@SuppressWarnings("rawtypes")
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				return super.completionSuggestions(cmdContext, cmdClass, argStrings);
			} else if(argStrings.size() == 1) {
				return CommandUtils.runListCommand(cmdContext, Module.class, new SelectQuery(Module.class)).
						getColumnValues(Module.NAME_PROPERTY);
			} else { 
				return Collections.emptyList();
			}
		}
		
		
		
	}

	
}
