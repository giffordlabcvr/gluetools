package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"show", "statistics"}, 
		docoptUsages={"[<memberStatistic> ...]"},
		description="Show statistics for this member", 
		furtherHelp="If no specific memberStatistics are supplied, all possible statistics are calculated") 
public class MemberShowStatisticsCommand extends MemberModeCommand<MemberShowStatisticsCommand.MemberStatisticsResult> {

	private List<AlignmentMember.MemberStatistic> memberStatistics;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		List<Element> memberStatElems = PluginUtils.findConfigElements(configElem, "memberStatistic");
		memberStatistics = memberStatElems.stream().
				map(elem -> PluginUtils.configureEnum(AlignmentMember.MemberStatistic.class, elem, "text()", true))
				.collect(Collectors.toList());
		if(memberStatistics.isEmpty()) {
			memberStatistics = Arrays.asList(AlignmentMember.MemberStatistic.values());
		}
	}


	@Override
	public MemberStatisticsResult execute(CommandContext cmdContext) {
		AlignmentMember almtMember = GlueDataObject.lookup(
				cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(getAlignmentName(), getSourceName(), getSequenceID()));
		return new MemberStatisticsResult(almtMember.getStatistics(memberStatistics, cmdContext));
	}
	
	
	public static class MemberStatisticsResult extends MapResult {

		protected MemberStatisticsResult(Map<String, Object> results) {
			super("memberStatisticsResult", results);
		}

	}

	@CompleterClass
	public static class MemberStatisticCompleter extends AdvancedCmdCompleter {

		public MemberStatisticCompleter() {
			super();
			registerEnumLookup("memberStatistic", AlignmentMember.MemberStatistic.class);
		}

		
	}


}
