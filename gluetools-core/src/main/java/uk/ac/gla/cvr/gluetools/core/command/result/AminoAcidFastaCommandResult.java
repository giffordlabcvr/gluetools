package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.Map;

import org.biojava.nbio.core.sequence.ProteinSequence;

import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class AminoAcidFastaCommandResult extends BaseFastaCommandResult {

	public AminoAcidFastaCommandResult(Map<String, ProteinSequence> fastaMap) {
		super(FastaUtils.proteinFastaMapToCommandDocument(fastaMap));
	}

}
