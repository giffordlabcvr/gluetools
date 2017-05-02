package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.protein;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein.FastaProteinAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.AbstractConsensusGenerator;
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
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="aminoAcidConsensusGenerator")
public class AminoAcidConsensusGenerator extends AbstractConsensusGenerator<AminoAcidConsensusGenerator>{

	public AminoAcidConsensusGenerator() {
		super();
		addModulePluginCmdClass(GenerateFastaAaConsensusCommand.class);
	}

	public CommandResult doGenerate(ConsoleCommandContext cmdContext,
			String fileName, String alignmentName,
			Optional<Expression> whereClause, SimpleAlignmentColumnsSelector alignmentColumnsSelector, 
			Boolean recursive, Boolean preview,
			String consensusID, LineFeedStyle lineFeedStyle) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		Map<Map<String, String>, String> memberPkMapToAlmtRow = 
				FastaProteinAlignmentExporter.exportAlignment(cmdContext, alignmentColumnsSelector, null, false, alignment, almtMembers);
		
		if(memberPkMapToAlmtRow.isEmpty()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No alignment members selected");
		}
		
		List<String> almtRows = new ArrayList<String>(memberPkMapToAlmtRow.values());

		Function<Character,Boolean> validChar = TranslationUtils::isAminoAcid;
		char unknownChar = 'X';
		String consensusFasta = generateConsensusFasta(almtRows, consensusID, validChar, unknownChar, lineFeedStyle);
		return formResult(cmdContext, consensusFasta, fileName, preview);
	}

}
