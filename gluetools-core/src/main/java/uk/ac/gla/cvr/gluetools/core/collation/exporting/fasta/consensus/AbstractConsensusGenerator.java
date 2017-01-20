package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus;

import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.procedure.TCharIntProcedure;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;


public abstract class AbstractConsensusGenerator<A extends AbstractConsensusGenerator<A>> extends ModulePlugin<A> {

	
	public static final String MINIMUM_ROWS = "minimumRows";

	// minimum number of rows where a value must be present in order to generate a consensus, otherwise N (nt) or X (aa) is generated.
	private Integer minimumRows;

	protected AbstractConsensusGenerator() {
		super();
		addSimplePropertyName(MINIMUM_ROWS);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.minimumRows = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MINIMUM_ROWS, 1, true, null, false, false)).orElse(1);
	}

	protected String generateConsensusFasta(List<String> almtRows, String consensusID,
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
		return FastaUtils.seqIdCompoundsPairToFasta(consensusID, consensus.toString());
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

	protected CommandResult formResult(ConsoleCommandContext cmdContext,
			String fastaString, String fileName, Boolean preview) {
		if(preview) {
			return new SimpleConsoleCommandResult(fastaString, false);
		} else {
			byte[] bytes = fastaString.getBytes();
			cmdContext.saveBytes(fileName, bytes);
			return new OkResult();
		}
	}
	
}
