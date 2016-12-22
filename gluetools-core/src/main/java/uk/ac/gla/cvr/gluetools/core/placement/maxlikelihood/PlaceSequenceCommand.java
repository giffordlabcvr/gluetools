package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer.PlacerResultInternal;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"place", "sequence"}, 
		description = "Place one or more stored sequences into a phylogeny", 
		docoptUsages = { "(-w <whereClause> | -a) [-b <batchSize>] [-d <dataDir>] -o <outputFilePrefix>" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>  Qualify the sequences to be placed",
				"-a, --allSequences                             Place all sequences in the project",
				"-b <batchSize>, --batchSize <batchSize>        Batch size",
				"-o <outputFile>, --outputFile <outputFile>     Output file path for placement results",
				"-d <dataDir>, --dataDir <dataDir>              Save algorithmic data in this directory",
		},
		furtherHelp = "Where the operation happens in multiple batches, separate algorithmic data directories "
			+"will be used for each batch.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class PlaceSequenceCommand extends AbstractPlaceCommand<OkResult> {

	public final static String WHERE_CLAUSE = "whereClause";
	public final static String ALL_SEQUENCES = "allSequences";
	public static final String BATCH_SIZE = "batchSize";
	public static final String FETCH_LIMIT = "fetchLimit";
	public static final String FETCH_OFFSET = "fetchOffset";

	
	private Expression whereClause;
	private Boolean allSequences;
	private int batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.allSequences = PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, false);
		if(this.whereClause == null && this.allSequences == null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or --allSequences must be specified");
		}
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);

	}

	
	@Override
	protected OkResult execute(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		SelectQuery selectQuery;
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		if(this.allSequences) {
			selectQuery = new SelectQuery(Sequence.class);
		} else {
			selectQuery = new SelectQuery(Sequence.class, this.whereClause);
		}
		int numSequences = GlueDataObject.count(cmdContext, selectQuery);
		selectQuery.setPageSize(batchSize);
		selectQuery.setFetchLimit(batchSize);
		int offset = 0;
		PlacerResultInternal fullResult = null;
		while(offset < numSequences) {
			selectQuery.setFetchOffset(offset);
			int firstBatchIndex = offset+1;
			int lastBatchIndex = Math.min(offset+batchSize, numSequences);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Retrieving sequences "+firstBatchIndex+" to "+lastBatchIndex+" of "+numSequences);
			List<Sequence> currentBatch = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Processing sequences "+firstBatchIndex+" to "+lastBatchIndex+" of "+numSequences);
			Map<String, DNASequence> querySequenceMap = new LinkedHashMap<String, DNASequence>();
			currentBatch.forEach(seq -> {
				querySequenceMap.put(seq.getSource().getName()+"/"+seq.getSequenceID(), 
						FastaUtils.ntStringToSequence(seq.getSequenceObject().getNucleotides(cmdContext)));
			});
			String requestedDataDir = getDataDir();
			File dataDirFile = null;
			if(batchSize >= numSequences) {
				dataDirFile = CommandUtils.ensureDataDir(consoleCmdContext, requestedDataDir);
			} else {
				dataDirFile = CommandUtils.ensureDataDir(consoleCmdContext, requestedDataDir+"_"+firstBatchIndex+"_"+lastBatchIndex);
			}
			PlacerResultInternal batchResult = maxLikelihoodPlacer.place(consoleCmdContext, querySequenceMap, dataDirFile);
			if(fullResult == null) {
				fullResult = batchResult;
			} else {
				fullResult.getQueryResults().putAll(batchResult.getQueryResults());
			}
			offset = offset+batchSize;
			cmdContext.newObjectContext();
		}
		GlueLogger.getGlueLogger().log(Level.FINEST, "Processed "+numSequences+" sequences ");
		CommandDocument placerResultCmdDocument = PojoDocumentUtils.pojoToCommandDocument(fullResult.toPojoResult());
		Document placerResultXmlDoc = CommandDocumentXmlUtils.commandDocumentToXmlDocument(placerResultCmdDocument);
		byte[] placerResultXmlBytes = GlueXmlUtils.prettyPrint(placerResultXmlDoc);
		consoleCmdContext.saveBytes(getOutputFile(), placerResultXmlBytes);
		GlueLogger.getGlueLogger().log(Level.FINEST, "Processed "+numSequences+" sequences ");
		return new OkResult();
	}


	@CompleterClass
	public static class Completer extends AbstractPlaceCommandCompleter {
		public Completer() {
			super();
		}
	}
	
}
