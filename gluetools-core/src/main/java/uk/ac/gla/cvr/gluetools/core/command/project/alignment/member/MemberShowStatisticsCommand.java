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
