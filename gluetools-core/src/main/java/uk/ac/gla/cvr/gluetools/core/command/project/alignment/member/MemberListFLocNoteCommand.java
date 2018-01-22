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
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractListCTableCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractListCTableCommand.AbstractListCTableDelegate;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass(
		commandWords={"list", "member-floc-note"}, 
		docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
		description="List member-featureLoc notes defined on this variation",
		furtherHelp=
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional whereClause qualifies which alignments are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list member-floc-note -w \"featureLoc.feature.name = 'NS3'\"\n"+
		"  list member-floc-note -w \"custom_field = 'value1'\"\n"+
		"  list member-floc-note featureLoc.feature.name custom_field")
public class MemberListFLocNoteCommand extends MemberModeCommand<ListResult> {
	
	private AbstractListCTableDelegate listCTableDelegate = new AbstractListCTableDelegate();
	
	
	public MemberListFLocNoteCommand() {
		super();
		listCTableDelegate.setTableName(ConfigurableTable.member_floc_note.name());
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		listCTableDelegate.configure(pluginConfigContext, configElem);
		Optional<Expression> whereClause = listCTableDelegate.getWhereClause();
		Expression pathExp = 
				ExpressionFactory.matchExp(MemberFLocNote.ALIGNMENT_NAME_PATH, getAlignmentName()).andExp(
				ExpressionFactory.matchExp(MemberFLocNote.SOURCE_NAME_PATH, getSourceName()).andExp(
				ExpressionFactory.matchExp(MemberFLocNote.SEQUENCE_ID_PATH, getSequenceID())));
		if(whereClause.isPresent()) {
			whereClause = Optional.of(whereClause.get().andExp(pathExp));
		} else {
			whereClause = Optional.of(pathExp);
		}
		listCTableDelegate.setWhereClause(whereClause);
		if(listCTableDelegate.getFieldNames() == null) {
			listCTableDelegate.setFieldNames(
					Arrays.asList(MemberFLocNote.REF_SEQ_NAME_PATH, MemberFLocNote.FEATURE_NAME_PATH));
		}
	}

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return listCTableDelegate.execute(cmdContext);
	}
	
	@CompleterClass
	public static class Completer extends AbstractListCTableCommand.ListCommandCompleter {
		public Completer() {
			super(ConfigurableTable.member_floc_note.name());
		}
	}


	
}
