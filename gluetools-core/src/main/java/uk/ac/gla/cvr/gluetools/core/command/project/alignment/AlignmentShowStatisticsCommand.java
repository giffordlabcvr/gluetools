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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"show", "statistics"}, 
		docoptUsages={"[<memberStatistic> ...]"},
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
		Alignment alignment = lookupAlignment(cmdContext);
		List<AlignmentMember> members = alignment.getMembers();
		List<Map<String, Object>> rows = members.stream()
				.map(memb -> {
					Map<String, Object> row = memb.getStatistics(memberStatistics, cmdContext);
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
	public static class MemberStatisticCompleter extends AdvancedCmdCompleter {

		public MemberStatisticCompleter() {
			super();
			registerEnumLookup("memberStatistic", AlignmentMember.MemberStatistic.class);
		}
		
		
	}
}
