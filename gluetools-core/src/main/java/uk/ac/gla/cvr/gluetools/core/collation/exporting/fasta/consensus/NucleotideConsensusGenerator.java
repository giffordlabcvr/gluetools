package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@PluginClass(elemName="nucleotideConsensusGenerator")
public class NucleotideConsensusGenerator extends AbstractConsensusGenerator<NucleotideConsensusGenerator> {

	public NucleotideConsensusGenerator() {
		super();
		addModulePluginCmdClass(GenerateFastaNtConsensusCommand.class);
	}

	public CommandResult doGenerate(ConsoleCommandContext cmdContext,
			String fileName, String alignmentName,
			Optional<Expression> whereClause, String relRefName,
			String featureName, Boolean recursive, Boolean preview,
			String lcStart, String lcEnd,
			Integer ntStart, Integer ntEnd, 
			String consensusID) {

		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		Map<Map<String, String>, DNASequence> memberPkMapToAlmtRow = 
				FastaAlignmentExporter.exportAlignment(cmdContext, relRefName, featureName, false, null, true, null, 
						lcStart, lcEnd, ntStart, ntEnd, 
						alignment, almtMembers);
		
		if(memberPkMapToAlmtRow.isEmpty()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No alignment members selected");
		}
		
		List<String> almtRows = memberPkMapToAlmtRow.values().stream()
				.map(dna -> dna.getSequenceAsString())
				.collect(Collectors.toList());

		Function<Character,Boolean> validChar = TranslationUtils::isNucleotide;
		char unknownChar = 'N';
		String consensusFasta = generateConsensusFasta(almtRows, consensusID, validChar, unknownChar);
		return formResult(cmdContext, consensusFasta, fileName, preview);
	}

	
	
	
}
