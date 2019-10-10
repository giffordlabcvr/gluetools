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
package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@CommandClass(
		commandWords={"recognise", "sequence"}, 
		description = "Apply recognition to stored sequences", 
		docoptUsages = { "(-w <whereClause> | -a)" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>  Qualify the sequences to be recognised",
				"-a, --allSequences                             Recognise all sequences in the project"
		},
		metaTags = {}	
		)
public class RecogniseSequenceCommand extends ModulePluginCommand<BlastSequenceRecogniserResult, BlastSequenceRecogniser> 
	implements ProvidedProjectModeCommand {

	public final static String WHERE_CLAUSE = "whereClause";
	public final static String ALL_SEQUENCES = "allSequences";

	private Expression whereClause;
	private Boolean allSequences;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.allSequences = PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, false);
		if(this.whereClause == null && this.allSequences == null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or --allSequences must be specified");
		}
	}

	@Override
	protected BlastSequenceRecogniserResult execute(CommandContext cmdContext, BlastSequenceRecogniser blastSequenceRecogniser) {
		SelectQuery selectQuery;
		if(this.allSequences) {
			selectQuery = new SelectQuery(Sequence.class);
		} else {
			selectQuery = new SelectQuery(Sequence.class, this.whereClause);
		}
		
		
		List<BlastSequenceRecogniserResultRow> resultRows = new ArrayList<BlastSequenceRecogniserResultRow>();
		
		int totalNumSeqs = GlueDataObject.count(cmdContext, selectQuery);
		int batchSize = 200;
		int processed = 0;
		int offset = 0;

		while(processed < totalNumSeqs) {
			selectQuery.setFetchLimit(batchSize);
			selectQuery.setPageSize(batchSize);
			selectQuery.setFetchOffset(offset);
			GlueLogger.getGlueLogger().finest("Retrieving sequences");
			List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);

			Map<String, DNASequence> querySequenceMap = new LinkedHashMap<String, DNASequence>();
			sequences.forEach(seq -> {
				querySequenceMap.put(seq.getSource().getName()+"/"+seq.getSequenceID(), 
						FastaUtils.ntStringToSequence(seq.getSequenceObject().getNucleotides(cmdContext)));
			});
			GlueLogger.getGlueLogger().finest("Recognising "+sequences.size()+" sequences");
			Map<String, List<RecognitionCategoryResult>> queryIdToCatResult = blastSequenceRecogniser.recognise(cmdContext, querySequenceMap);
			resultRows.addAll(BlastSequenceRecogniserResultRow.rowsFromMap(queryIdToCatResult));

			offset += batchSize;
			processed += sequences.size();
			GlueLogger.getGlueLogger().finest("Processed "+processed+" of "+totalNumSeqs+" sequences");
			cmdContext.newObjectContext();
		}
		return new BlastSequenceRecogniserResult(resultRows);
		
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		
	}

}
