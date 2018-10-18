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

import java.util.List;
import java.util.function.Function;

import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.procedure.TCharIntProcedure;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
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
