package uk.ac.gla.cvr.gluetools.utils;

public class StringUtils {

	public static boolean charSequencesEqual(CharSequence seq1, CharSequence seq2) {
		if(seq1.length() != seq2.length()) {
			return false;
		}
		for(int i = 0; i < seq1.length(); i++) {
			if(seq1.charAt(i) != seq2.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	public static String reverseString(String inputString) {
		return new StringBuilder(inputString).reverse().toString();
	}
	
}
