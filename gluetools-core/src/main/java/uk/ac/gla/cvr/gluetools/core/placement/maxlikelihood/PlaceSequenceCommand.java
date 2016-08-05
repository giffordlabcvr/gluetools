package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"place", "sequence"}, 
		description = "Place one or more stored sequences into a phylogeny", 
		docoptUsages = { "(-w <whereClause> | -a) [-d <dataDir>]" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>  Qualify the sequences to be placed",
				"-a, --allSequences                             Place all sequences in the project",
				"-d <dataDir>, --dataDir <dataDir>              Save algorithmic data in this directory",
		},
		furtherHelp = "",
		metaTags = {}	
)
public class PlaceSequenceCommand extends AbstractPlaceCommand {

	public final static String WHERE_CLAUSE = "whereClause";
	public final static String ALL_SEQUENCES = "allSequences";
	public final static String DATA_DIR = "dataDir";
	
	private Expression whereClause;
	private Boolean allSequences;
	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.allSequences = PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, false);
		if(this.whereClause == null && this.allSequences == null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or --allSequences must be specified");
		}
		this.dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
	}

	@Override
	protected PlaceCommandResult execute(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		SelectQuery selectQuery;
		if(this.allSequences) {
			selectQuery = new SelectQuery(Sequence.class);
		} else {
			selectQuery = new SelectQuery(Sequence.class, this.whereClause);
		}
		List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
		Map<String, DNASequence> querySequenceMap = new LinkedHashMap<String, DNASequence>();
		sequences.forEach(seq -> {
			querySequenceMap.put(seq.getSource().getName()+"/"+seq.getSequenceID(), 
					FastaUtils.ntStringToSequence(seq.getSequenceObject().getNucleotides(cmdContext)));
		});
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);
		Map<String, List<PlacementResult>> seqNameToPlacementResults = maxLikelihoodPlacer.place(cmdContext, querySequenceMap, dataDirFile);
		return generatePlaceCommandResult(seqNameToPlacementResults);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("dataDir", true);
		}
	}
	
}
