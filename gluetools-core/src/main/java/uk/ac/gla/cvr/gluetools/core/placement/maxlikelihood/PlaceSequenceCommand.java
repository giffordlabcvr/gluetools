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
		docCategory = "Type-specific module commands",
		docoptUsages = { "(-w <whereClause> | -a) [-p <pageSize>] [-l <fetchLimit>] [-f <fetchOffset>] [-d <dataDir>] -o <outputFile>" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>  Qualify the sequences to be placed",
				"-a, --allSequences                             Place all sequences in the project",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
				"-f <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset",
				"-o <outputFile>, --outputFile <outputFile>     Output file path for placement results",
				"-d <dataDir>, --dataDir <dataDir>              Save algorithmic data in this directory",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class PlaceSequenceCommand extends AbstractPlaceCommand<OkResult> {

	public final static String WHERE_CLAUSE = "whereClause";
	public final static String ALL_SEQUENCES = "allSequences";
	public static final String PAGE_SIZE = "pageSize";
	public static final String FETCH_LIMIT = "fetchLimit";
	public static final String FETCH_OFFSET = "fetchOffset";

	
	private Expression whereClause;
	private Boolean allSequences;
	private Optional<Integer> fetchLimit;
	private Optional<Integer> fetchOffset;
	private int pageSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.allSequences = PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, false);
		if(this.whereClause == null && this.allSequences == null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or --allSequences must be specified");
		}
		pageSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, PAGE_SIZE, false)).orElse(250);
		fetchLimit = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_LIMIT, false));
		fetchOffset = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_OFFSET, false));


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
		selectQuery.setPageSize(pageSize);
		fetchLimit.ifPresent(limit -> selectQuery.setFetchLimit(limit));
		fetchOffset.ifPresent(offset -> selectQuery.setFetchOffset(offset));
		GlueLogger.getGlueLogger().log(Level.FINEST, "Retrieving sequences");
		List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
		GlueLogger.getGlueLogger().log(Level.FINEST, "Retrieved "+sequences.size()+" sequences, processing...");
		Map<String, DNASequence> querySequenceMap = new LinkedHashMap<String, DNASequence>();
		sequences.forEach(seq -> {
			querySequenceMap.put(seq.getSource().getName()+"/"+seq.getSequenceID(), 
					FastaUtils.ntStringToSequence(seq.getSequenceObject().getNucleotides(cmdContext)));
		});
		String requestedDataDir = getDataDir();
		File dataDirFile = CommandUtils.ensureDataDir(consoleCmdContext, requestedDataDir);
		PlacerResultInternal placerResultInternal = maxLikelihoodPlacer.place(consoleCmdContext, querySequenceMap, dataDirFile);
		CommandDocument placerResultCmdDocument = PojoDocumentUtils.pojoToCommandDocument(placerResultInternal.toPojoResult());
		Document placerResultXmlDoc = CommandDocumentXmlUtils.commandDocumentToXmlDocument(placerResultCmdDocument);
		byte[] placerResultXmlBytes = GlueXmlUtils.prettyPrint(placerResultXmlDoc);
		String outputFile = getOutputFile();
		consoleCmdContext.saveBytes(outputFile, placerResultXmlBytes);
		GlueLogger.getGlueLogger().log(Level.FINEST, "Saved placerResult to "+outputFile);
		return new OkResult();
	}


	@CompleterClass
	public static class Completer extends AbstractPlaceCommandCompleter {
		public Completer() {
			super();
		}
	}
	
}
