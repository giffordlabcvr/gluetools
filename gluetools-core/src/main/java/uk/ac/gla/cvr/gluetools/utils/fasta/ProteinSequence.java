package uk.ac.gla.cvr.gluetools.utils.fasta;

import uk.ac.gla.cvr.gluetools.core.translation.ResidueUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtilsException;
import uk.ac.gla.cvr.gluetools.utils.FastaUtilsException.Code;

public class ProteinSequence extends AbstractSequence {

	private static final int MAX_CHAR = 256;

	private String validatedString;
	
	private static boolean[] charValid = new boolean[MAX_CHAR];
	static {
		for(int i = 0; i < ResidueUtils.ALL_AAS.length(); i++) {
			charValid[ResidueUtils.ALL_AAS.charAt(i)] = true;
		}
		// gap character
		charValid['-'] = true;
	}
	
	public ProteinSequence(String aaString) {
		String aaStringUpper = aaString.toUpperCase();
		for(int i = 0; i < aaStringUpper.length(); i++) {
			char stringChar = aaStringUpper.charAt(i);
			if(stringChar != 'X' && ( stringChar < 0 || stringChar >= MAX_CHAR || !charValid[stringChar])) {
				throw new FastaUtilsException(Code.INVALID_AMINO_ACID_CHARACTER, "'"+new String(new char[] {stringChar})+"'", Integer.toString(i+1));
			}
		}
		this.validatedString = aaString;
	}
	
	public String toString() {
		return validatedString;
	}
}
