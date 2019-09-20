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
package uk.ac.gla.cvr.gluetools.core.curation.sequenceMergers;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSourceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"merge", "almt-rows"}, 
		description = "Generate sequences by merging alignment rows", 
		docoptUsages = { "<almtName> (-a | -w <whereClause>)" },
		docoptOptions = { 
				"-a, --allRows                                  Select all alignment rows",
				"-w <whereClause>, --whereClause <whereClause>  Qualify selected rows",
		},
		furtherHelp = ""	
)
public class MergeRowsCommand extends ModulePluginCommand<CreateResult, AlignmentBasedSequenceMerger> {

	private String alignmentName;
	private Expression whereClause;
	private boolean allRows;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, "almtName", true);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
		this.allRows = PluginUtils.configureBooleanProperty(configElem, "allRows", false);
		
		if(! (
				(whereClause != null && !allRows) ||
				(whereClause == null && allRows)
			) ) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either --allRows or --whereClause must be specified, but not both");
		}
	}

	@Override
	protected CreateResult execute(CommandContext cmdContext, AlignmentBasedSequenceMerger sequenceMerger) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		
		List<AlignmentMember> selectedMembers;
		if(allRows) {
			selectedMembers = alignment.getMembers();
		} else {
			Expression exp = whereClause.andExp(ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignmentName));
			selectedMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, exp));
		}
		Map<String, DNASequence> seqIdToNTs = sequenceMerger.doMergeAlignmentRows(cmdContext, selectedMembers);

		Source source = CreateSourceCommand.createSource(cmdContext, sequenceMerger.getSourceName(), true);
		
		seqIdToNTs.forEach((sequenceID, nts) -> {
			String fastaString = ">"+sequenceID+"\n"+nts.getSequenceAsString()+"\n";
			byte[] fastaBytes = fastaString.getBytes();
			String sourceName = sequenceMerger.getSourceName();
			Sequence sequence = CreateSequenceCommand.createSequence(cmdContext, sourceName, sequenceID, false);
			sequence.setSource(source);
			sequence.setFormat(SequenceFormat.FASTA.name());
			sequence.setOriginalData(fastaBytes);
		});
		cmdContext.commit();
		
		return new CreateResult(Sequence.class, seqIdToNTs.size());
	}
	
	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("almtName", Alignment.class, Alignment.NAME_PROPERTY);
		}
		
	}

}
