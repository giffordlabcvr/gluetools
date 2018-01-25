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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider.SourceInfoProvider;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;


@CommandClass(
		commandWords={"generate-sqn"}, 
		description = "Generate a .sqn submission file from a set of stored GLUE sequences", 
		docoptUsages = { "(-w <whereClause> | -a) -t <templateFile> [-o <outputDir>] [-d <dataDir>]" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>     Qualify the sequence set",
				"-a, --allSequences                                All sequences in the project",
				"-t <templateFile>, --templateFile <templateFile>  Template .sbt file",
				"-o <outputDir>, --outputDir <outputDir>           Directory for .sqn files",
				"-d <dataDir>, --dataDir <dataDir>                 Directory for intermediate files",
		},
		furtherHelp = "This command uses tbl2asn as a subroutine to generate .sqn files for GenBank submssion. "+
		"If <outputDir> is omitted, the .sqn files are written to the current load/save path. If <outputDir> does not "+
		"exist, it is created. If <dataDir> is supplied, it is created if it does not exist and the the intermediate "+
		"files which were supplied to tbl2asn are retained in that directory.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class GenerateSqnCommand extends ModulePluginCommand<GenerateSqnResult, GbSubmisisonGenerator> {

	
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
		this.outputDir = PluginUtils.configureStringProperty(configElem, OUTPUT_DIR, false);
		this.dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
	}
	
	
	@Override
	protected GenerateSqnResult execute(CommandContext cmdContext, GbSubmisisonGenerator gbSubmisisonGenerator) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		byte[] templateBytes = consoleCmdContext.loadBytes(templateFile);

		File dataDirFile = null;
		if(dataDir != null) {
			dataDirFile = consoleCmdContext.fileStringToFile(dataDir);
			dataDirFile.mkdirs();
			if(!dataDirFile.exists()) {
				throw new Tbl2AsnException(Tbl2AsnException.Code.TBL2ASN_FILE_EXCEPTION, 
						"Unable to create data directory "+dataDirFile.getAbsolutePath());

			}
		}
		
		File outputDirFile;
		
		if(outputDir == null) {
			outputDirFile = consoleCmdContext.getLoadSavePath();
		} else {
			outputDirFile = consoleCmdContext.fileStringToFile(outputDir);
			outputDirFile.mkdirs();
			if(!outputDirFile.exists()) {
				throw new Tbl2AsnException(Tbl2AsnException.Code.TBL2ASN_FILE_EXCEPTION, 
						"Unable to create output directory "+outputDirFile.getAbsolutePath());

			}
		}		
					
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

		List<SequenceSqnResult> sequenceSqnResults = new ArrayList<SequenceSqnResult>();
		LinkedHashSet<String> generatedIDs = new LinkedHashSet<String>();

		while(processed < totalNumSeqs) {
			selectQuery.setFetchLimit(batchSize);
			selectQuery.setPageSize(batchSize);
			selectQuery.setFetchOffset(offset);
			GlueLogger.getGlueLogger().finest("Retrieving sequences");
			List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);

			List<Tbl2AsnInput> inputs = new ArrayList<Tbl2AsnInput>();
			
			List<SourceInfoProvider> sourceInfoProviders = gbSubmisisonGenerator.getSourceInfoProviders();
			List<String> sourceColumnHeaders = new ArrayList<String>();
			sourceColumnHeaders.add("SeqID");
			sourceInfoProviders.forEach(sip -> sourceColumnHeaders.add(sip.getSourceModifier()));
			
			boolean sourceInfo = !sourceInfoProviders.isEmpty();
			
			sequences.forEach(seq -> {
				String id = gbSubmisisonGenerator.generateId(seq);
				if(generatedIDs.contains(id)) {
					throw new Tbl2AsnException(Tbl2AsnException.Code.TBL2ASN_DATA_EXCEPTION, 
							"Duplicate ID string '"+id+"' was generated for different sequences");
				}
				generatedIDs.add(id);
				Map<String, String> sourceInfoMap = new LinkedHashMap<String, String>();
				sourceInfoMap.put("SeqID", id);
				for(SourceInfoProvider sourceInfoProvider: sourceInfoProviders) {
					sourceInfoMap.put(sourceInfoProvider.getSourceModifier(), sourceInfoProvider.provideSourceInfo(seq));
				}
				inputs.add(new Tbl2AsnInput(seq.getSource().getName(), seq.getSequenceID(), id, 
					FastaUtils.ntStringToSequence(seq.getSequenceObject().getNucleotides(cmdContext)), 
					sourceInfoMap));
			});

			List<Tbl2AsnResult> batchResults = gbSubmisisonGenerator.getTbl2AsnRunner().
					generateSqnFiles(consoleCmdContext, sourceColumnHeaders, inputs, templateBytes, dataDirFile, sourceInfo);
			
			batchResults.forEach(result -> {
				File resultFile = new File(outputDirFile, result.getId()+".sqn");
				ConsoleCommandContext.saveBytesToFile(resultFile, result.getSqnFileContent());
				sequenceSqnResults.add(new SequenceSqnResult(result.getSourceName(), result.getSequenceID(), resultFile.getAbsolutePath()));
			});
			
			
			offset += batchSize;
			processed += sequences.size();
			GlueLogger.getGlueLogger().finest("Processed "+processed+" of "+totalNumSeqs+" sequences");
			cmdContext.newObjectContext();
		}
		return new GenerateSqnResult(sequenceSqnResults);
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
	
	/**
	 * summary for a single sequence.
	 */
	public static class SequenceSqnResult {
		
		private String sourceName;
		private String sequenceID;
		private String filePath;
		private SequenceSqnResult(String sourceName, String sequenceID,
				String filePath) {
			super();
			this.sourceName = sourceName;
			this.sequenceID = sequenceID;
			this.filePath = filePath;
		}
		public String getSourceName() {
			return sourceName;
		}
		public String getSequenceID() {
			return sequenceID;
		}
		public String getFilePath() {
			return filePath;
		}
	}
	
}
