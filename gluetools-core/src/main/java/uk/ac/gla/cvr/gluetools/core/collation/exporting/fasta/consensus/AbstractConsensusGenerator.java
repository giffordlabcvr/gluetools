package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus;

import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.procedure.TCharIntProcedure;

import java.util.List;
import java.util.function.Function;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;


public abstract class AbstractConsensusGenerator<A extends AbstractConsensusGenerator<A>> extends ModulePlugin<A> {

	
	protected AbstractConsensusGenerator() {
		super();
	}

	protected String generateConsensusFasta(List<String> almtRows,
			Function<Character, Boolean> validChar, char unknownChar) {
		int almtWidth = almtRows.get(0).length();
		TCharIntHashMap[] posToCharToOccurences = new TCharIntHashMap[almtWidth];
		for(int i = 0; i < almtWidth; i++) {
			posToCharToOccurences[i] = new TCharIntHashMap();
		}
		for(String almtRow: almtRows) {
			for(int i = 0; i < almtWidth; i++) {
				char c = almtRow.charAt(i);
				if(validChar.apply(c)) {
					posToCharToOccurences[i].adjustOrPutValue(c, 1, 1);
				}
			}
		}
		StringBuffer consensus = new StringBuffer();
		HighestOccuringCharProcedure procedure = new HighestOccuringCharProcedure();
		for(int i = 0; i < almtWidth; i++) {
			procedure.highestOccurences = null;
			procedure.highestOccuringChar = null;
			posToCharToOccurences[i].forEachEntry(procedure);
			if(procedure.highestOccuringChar != null) {
				consensus.append(procedure.highestOccuringChar);
			} else {
				consensus.append(unknownChar);
			}
		}
		return consensus.toString();
	}

	private class HighestOccuringCharProcedure implements TCharIntProcedure {
		public Integer highestOccurences = null;
		public Character highestOccuringChar = null;
		@Override
		public boolean execute(char c, int occurences) {
			if(highestOccurences == null || occurences > highestOccurences || 
				( highestOccurences.equals(occurences) && Character.compare(c, highestOccuringChar) > 0) ) {
				highestOccurences = occurences;
				highestOccuringChar = c;
			}
			return true;
		}
	}

	protected OkResult formResult(ConsoleCommandContext cmdContext,
			String consensusId, String consensusString, String fileName, LineFeedStyle lineFeedStyle) {
		byte[] bytes = FastaUtils.seqIdCompoundsPairToFasta(consensusId, consensusString, lineFeedStyle).getBytes();
		cmdContext.saveBytes(fileName, bytes);
		return new OkResult();
	}
	
}
