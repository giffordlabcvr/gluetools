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
package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"generate-sqn"}, 
		description = "Generate a .sqn submission file from a set of stored GLUE sequences", 
		docoptUsages = { "(-w <whereClause> | -a) -t <templateFile> -o <outputDir> [-d <dataDir>]" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>     Qualify the sequence set",
				"-a, --allSequences                                All sequences in the project",
				"-t <templateFile>, --templateFile <templateFile>  Template .sbt file",
				"-o <outputDir>, --outputDir <outputDir>           Directory for .sqn files",
				"-d <dataDir>, --dataDir <dataDir>                 Directory for intermediate files",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class GenerateSqnCommand extends ModulePluginCommand<OkResult, GbSubmisisonGenerator> {

	
	public final static String WHERE_CLAUSE = "whereClause";
	public final static String ALL_SEQUENCES = "allSequences";
	public final static String TEMPLATE_FILE = "templateFile";
	public final static String OUTPUT_DIR = "outputDir";
	public final static String DATA_DIR = "dataDir";
	
	private Expression whereClause;
	private Boolean allSequences;

	private String templateFile;
	private String outputDir;
	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.allSequences = PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, false);
		if(this.whereClause == null && this.allSequences == null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or --allSequences must be specified");
		}
		this.templateFile = PluginUtils.configureStringProperty(configElem, TEMPLATE_FILE, true);
		this.outputDir = PluginUtils.configureStringProperty(configElem, OUTPUT_DIR, true);
		this.dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
	}
	

	@Override
	protected OkResult execute(CommandContext cmdContext, GbSubmisisonGenerator gbSubmisisonGenerator) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		byte[] templateBytes = consoleCmdContext.loadBytes(templateFile);
		File dataDirFile = new File(dataDir);
		File outputDirFile = new File(outputDir);
		
		SelectQuery selectQuery;
		if(this.allSequences) {
			selectQuery = new SelectQuery(Sequence.class);
		} else {
			selectQuery = new SelectQuery(Sequence.class, this.whereClause);
		}
		
		int totalNumSeqs = GlueDataObject.count(cmdContext, selectQuery);
		int batchSize = 250;
		int processed = 0;
		int offset = 0;

		while(processed < totalNumSeqs) {
			selectQuery.setFetchLimit(batchSize);
			selectQuery.setPageSize(batchSize);
			selectQuery.setFetchOffset(offset);
			GlueLogger.getGlueLogger().finest("Retrieving sequences");
			List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);

			List<Tbl2AsnInput> inputs = new ArrayList<Tbl2AsnInput>();
			
			sequences.forEach(seq -> {
				inputs.add(
							new Tbl2AsnInput(gbSubmisisonGenerator.generateId(seq), 
									FastaUtils.ntStringToSequence(seq.getSequenceObject().getNucleotides(cmdContext))));
			});

			List<Tbl2AsnResult> batchResults = gbSubmisisonGenerator.getTbl2AsnRunner().generateSqnFiles(consoleCmdContext, inputs, templateBytes, dataDirFile);
			batchResults.forEach(result -> {
				File resultFile = new File(outputDirFile, result.getId()+".sqn");
				ConsoleCommandContext.saveBytesToFile(resultFile, result.getSqnFileContent());
			});
			
			
			offset += batchSize;
			processed += sequences.size();
			GlueLogger.getGlueLogger().finest("Processed "+processed+" of "+totalNumSeqs+" sequences");
			cmdContext.newObjectContext();
		}
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("templateFile", false);
			registerPathLookup("outputDir", true);
			registerPathLookup("dataDir", true);
		}
		
	}
	
}
