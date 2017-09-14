package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class NucleotideFastaCommandResult extends BaseFastaCommandResult {

	public NucleotideFastaCommandResult(Map<String, DNASequence> fastaMap) {
		super(FastaUtils.ntFastaMapToCommandDocument(fastaMap));
	}
}
