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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.NucleotideFastaCommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@PluginClass(elemName="nucleotideConsensusGenerator",
	description="Produces a consensus nucleotide sequence based on a configurable AlignmentMember set")
public class NucleotideConsensusGenerator extends AbstractConsensusGenerator<NucleotideConsensusGenerator> {

	public NucleotideConsensusGenerator() {
		super();
		registerModulePluginCmdClass(GenerateFastaNtConsensusCommand.class);
	}

	public CommandResult doGenerate(ConsoleCommandContext cmdContext,
			String fileName, String alignmentName,
			Optional<Expression> whereClause, IAlignmentColumnsSelector alignmentColumnsSelector,
			Boolean recursive, Boolean preview,
			String consensusID, LineFeedStyle lineFeedStyle) {

		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(alignmentName, recursive, whereClause);
		
		Map<Map<String, String>, DNASequence> memberPkMapToAlmtRow = 
				FastaAlignmentExporter.exportAlignment(cmdContext, alignmentColumnsSelector, true, 
						queryMemberSupplier);
		
		if(memberPkMapToAlmtRow.isEmpty()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No alignment members selected");
		}
		
		List<String> almtRows = memberPkMapToAlmtRow.values().stream()
				.map(dna -> dna.getSequenceAsString())
				.collect(Collectors.toList());

		Function<Character,Boolean> validChar = TranslationUtils::isNucleotide;
		char unknownChar = 'N';
		String consensusFasta = generateConsensusFasta(almtRows, validChar, unknownChar);
		if(preview) {
			Map<String, DNASequence> map = new LinkedHashMap<String, DNASequence>();
			map.put(consensusID, FastaUtils.ntStringToSequence(consensusFasta));
			return new NucleotideFastaCommandResult(map);
		} else {
			return formResult(cmdContext, consensusID, consensusFasta, fileName, lineFeedStyle);
		}
	}

	
	
	
}
