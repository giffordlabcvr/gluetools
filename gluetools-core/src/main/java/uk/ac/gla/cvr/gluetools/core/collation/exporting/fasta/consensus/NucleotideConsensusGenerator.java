package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="nucleotideConsensusGenerator")
public class NucleotideConsensusGenerator extends AbstractConsensusGenerator<NucleotideConsensusGenerator> {

	public NucleotideConsensusGenerator() {
		super();
		addModulePluginCmdClass(GenerateFastaNtConsensusCommand.class);
	}

	public CommandResult doGenerate(ConsoleCommandContext cmdContext,
			String fileName, String alignmentName,
			Optional<Expression> whereClause, IAlignmentColumnsSelector alignmentColumnsSelector,
			Boolean recursive, Boolean preview,
			String consensusID, LineFeedStyle lineFeedStyle) {

		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(alignmentName, recursive, whereClause);
		
		Map<Map<String, String>, DNASequence> memberPkMapToAlmtRow = 
				FastaAlignmentExporter.exportAlignment(cmdContext, alignmentColumnsSelector, true, null, 
						queryMemberSupplier);
		
		if(memberPkMapToAlmtRow.isEmpty()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No alignment members selected");
		}
		
		List<String> almtRows = memberPkMapToAlmtRow.values().stream()
				.map(dna -> dna.getSequenceAsString())
				.collect(Collectors.toList());

		Function<Character,Boolean> validChar = TranslationUtils::isNucleotide;
		char unknownChar = 'N';
		String consensusFasta = generateConsensusFasta(almtRows, consensusID, validChar, unknownChar, lineFeedStyle);
		return formResult(cmdContext, consensusFasta, fileName, preview);
	}

	
	
	
}
