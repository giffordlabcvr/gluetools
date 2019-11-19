package uk.ac.gla.cvr.gluetools.utils.fasta;

import uk.ac.gla.cvr.gluetools.core.translation.ResidueUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtilsException;
import uk.ac.gla.cvr.gluetools.utils.FastaUtilsException.Code;

public class DNASequence extends AbstractSequence {

	private static final int MAX_CHAR = 256;

	private String validatedString;
	
	private static boolean[] charValid = new boolean[MAX_CHAR];
	static {
		for(int i = 0; i < ResidueUtils.ALL_AMBIG_NTS.length(); i++) {
			charValid[ResidueUtils.ALL_AMBIG_NTS.charAt(i)] = true;
		}
		// gap character
		charValid['-'] = true;
	}
	
	public DNASequence(String ntString) {
		String ntStringUpper = ntString.toUpperCase();
		for(int i = 0; i < ntStringUpper.length(); i++) {
			char stringChar = ntStringUpper.charAt(i);
			if(stringChar < 0 || stringChar >= MAX_CHAR || !charValid[stringChar]) {
				throw new FastaUtilsException(Code.INVALID_NUCLEOTIDE_CHARACTER, "'"+new String(new char[] {stringChar})+"'", Integer.toString(i+1));
			}
		}
		this.validatedString = ntStringUpper;
	}
	
	public String toString() {
		return validatedString;
	}
}
