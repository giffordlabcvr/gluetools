package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

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
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.genotyping.GenotypingCommandResult;
import uk.ac.gla.cvr.gluetools.core.genotyping.GenotypingResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"genotype"}, 
		description = "Determine genotype of one or more sequences", 
		docoptUsages = { "(-w <whereClause> | -a)" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>  Qualify the sequences to be genotyped",
				"-a, --allSequences                             Genotype all sequences in the project",
		},
		furtherHelp = "",
		metaTags = {}	
)
public class GenotypeCommand extends ModulePluginCommand<GenotypingCommandResult, MaxLikelihoodGenotyper> {

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
	protected GenotypingCommandResult execute(CommandContext cmdContext, MaxLikelihoodGenotyper modulePlugin) {
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
		List<GenotypingResult> genotypingResults = modulePlugin.genotype(cmdContext, querySequenceMap);
		return new GenotypingCommandResult(genotypingResults);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		
	}
	
}
