package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterUtils;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"show", "statistics"}, 
		docoptUsages={"[<memberStatistic>...]"},
		description="Show statistics for the alignment's members", 
		furtherHelp="If no specific memberStatistics are supplied, all possible statistics are calculated") 
public class AlignmentShowStatisticsCommand extends AlignmentModeCommand<AlignmentShowStatisticsCommand.AlignmentCoverageResult> {

	private List<AlignmentMember.MemberStatistic> memberStatistics;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		List<Element> coverageStatElems = PluginUtils.findConfigElements(configElem, "memberStatistic");
		memberStatistics = coverageStatElems.stream().
				map(elem -> PluginUtils.configureEnum(AlignmentMember.MemberStatistic.class, elem, "text()", true))
				.collect(Collectors.toList());
		if(memberStatistics.isEmpty()) {
			memberStatistics = Arrays.asList(AlignmentMember.MemberStatistic.values());
		}
		
	}

	@Override
	public AlignmentCoverageResult execute(CommandContext cmdContext) {
		Alignment alignment = GlueDataObject.lookup(cmdContext.getObjectContext(), 
				Alignment.class, Alignment.pkMap(getAlignmentName()));
		List<AlignmentMember> members = alignment.getMembers();
		List<Map<String, Object>> rows = members.stream()
				.map(memb -> {
					Map<String, Object> row = memb.getStatistics(memberStatistics);
					row.put(AlignmentMember.SOURCE_NAME_PATH, memb.getSequence().getSource().getName());
					row.put(AlignmentMember.SEQUENCE_ID_PATH, memb.getSequence().getSequenceID());
					return row;
				})
				.collect(Collectors.toList());
		List<String> columnHeaders = new ArrayList<String>(Arrays.asList(AlignmentMember.SOURCE_NAME_PATH, AlignmentMember.SEQUENCE_ID_PATH));
		columnHeaders.addAll(memberStatistics.stream()
				.map(s -> s.name()).collect(Collectors.toList()));
		return new AlignmentCoverageResult(columnHeaders, rows);
	}

	public static class AlignmentCoverageResult extends TableResult {

		public AlignmentCoverageResult(List<String> columnHeaders, List<Map<String, Object>> rowData) {
			super("alignmentCoverageResult", columnHeaders, rowData);
		}
	}

	@CompleterClass
	public static class MemberStatisticCompleter extends CommandCompleter {

		@SuppressWarnings("rawtypes")
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			return CompleterUtils.enumCompletionSuggestions(AlignmentMember.MemberStatistic.class);
		}
		
	}
}
