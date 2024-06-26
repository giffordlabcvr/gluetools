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

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractCountCTableCommand.AbstractCountCTableDelegate;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractCountCTableCommand.CountCommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.result.CountResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"count", "member"},
	docoptUsages={"[-r] [-w <whereClause>]"},
	docoptOptions={
			"-r, --recursive                                         Include descendent members",
		    "-w <whereClause>, --whereClause <whereClause>           Qualify result set"},
	description="Count alignment members",
	furtherHelp=
	"The optional whereClause qualifies which alignment members are included.\n"+
	"Examples:\n"+
	"  count member -w \"sequence.source.name = 'local'\"\n"+
	"  count member -w \"sequence.custom_field = 'value1'\""
) 
public class AlignmentCountMemberCommand extends AlignmentModeCommand<CountResult> {
	
	public static final String RECURSIVE = "recursive";

	private AbstractCountCTableDelegate countCTableDelegate = new AbstractCountCTableDelegate();
	
	private Boolean recursive;

	public AlignmentCountMemberCommand() {
		super();
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		recursive = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, RECURSIVE, false)).orElse(false);
		countCTableDelegate.setTableName(ConfigurableTable.alignment_member.name());
		countCTableDelegate.configure(pluginConfigContext, configElem);
	}

	@CompleterClass
	public static final class Completer extends CountCommandCompleter {
		public Completer() {
			super(ConfigurableTable.alignment_member.name());
		}
	}

	@Override
	public CountResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		countCTableDelegate.setWhereClause(Optional.of(
				AlignmentListMemberCommand.getMatchExpression(alignment, recursive, countCTableDelegate.getWhereClause())));
		return countCTableDelegate.execute(cmdContext);
	}


}
