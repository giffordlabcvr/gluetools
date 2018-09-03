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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.protein;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.ProteinSequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractStringAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAminoAcidColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.AbstractConsensusGenerator;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.AminoAcidFastaCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="aminoAcidConsensusGenerator", 
		description="Produces a consensus amino acid sequence based on a configurable AlignmentMember set")
public class AminoAcidConsensusGenerator extends AbstractConsensusGenerator<AminoAcidConsensusGenerator>{

	public AminoAcidConsensusGenerator() {
		super();
		registerModulePluginCmdClass(GenerateFastaAaConsensusCommand.class);
	}

	public CommandResult doGenerate(ConsoleCommandContext cmdContext,
			String fileName, String alignmentName,
			Optional<Expression> whereClause, String featureName, SimpleAminoAcidColumnsSelector alignmentColumnsSelector, 
			Boolean recursive, Boolean preview,
			String consensusID, LineFeedStyle lineFeedStyle) {
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(alignmentName, recursive, whereClause);
		Map<Map<String,String>, String> memberPkMapToAlmtRow = new LinkedHashMap<Map<String, String>, String>();
		alignmentColumnsSelector.generateStringAlignmentRows(cmdContext, false, queryMemberSupplier,  
				new AbstractStringAlmtRowConsumer() {
					@Override
					public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember,
							String alignmentRowString) {
						memberPkMapToAlmtRow.put(almtMember.pkMap(), alignmentRowString);
					}
				});
		
		if(memberPkMapToAlmtRow.isEmpty()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No alignment members selected");
		}
		
		List<String> almtRows = new ArrayList<String>(memberPkMapToAlmtRow.values());
		Function<Character,Boolean> validChar = TranslationUtils::isAminoAcid;
		char unknownChar = 'X';
		String consensusFasta = generateConsensusFasta(almtRows, validChar, unknownChar);
		if(preview) {
			Map<String, ProteinSequence> map = new LinkedHashMap<String, ProteinSequence>();
			map.put(consensusID, FastaUtils.proteinStringToSequence(consensusFasta));
			return new AminoAcidFastaCommandResult(map);
		} else {
			return formResult(cmdContext, consensusID, consensusFasta, fileName, lineFeedStyle);
		}
	}

}
