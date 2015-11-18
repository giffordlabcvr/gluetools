package uk.ac.gla.cvr.gluetools.core.reporting.contentNotes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

/**
 * A reference difference note points out that the sequence content differs from a reference at certain locations.
 * For efficiency, these differences are encoded in a mask char sequence. In the mask, "-" represents no difference and 
 * any other character represents a difference (the value in the sequence content).
 */

public class ReferenceDifferenceNote extends SequenceContentNote {

	private CharSequence mask;
	private List<DifferenceSummaryNote> differenceSummaryNotes = new ArrayList<DifferenceSummaryNote>();

	public ReferenceDifferenceNote(int refStart, int refEnd, CharSequence refChars, CharSequence queryChars, 
			boolean includeDifferenceSummaryNotes, Map<Integer, List<VariationNote>> refStartToVariationNotes) {
		super(refStart, refEnd);
		init(refStart, refChars, queryChars, includeDifferenceSummaryNotes, refStartToVariationNotes);
	}

	@Override
	public void toDocument(ObjectBuilder sequenceDifferenceObj) {
		super.toDocument(sequenceDifferenceObj);
		sequenceDifferenceObj.set("mask", mask);
		if(differenceSummaryNotes != null) {
			ArrayBuilder diffSummaryArray = sequenceDifferenceObj.setArray("differenceSummaryNote");
			for(DifferenceSummaryNote diffSummNote: differenceSummaryNotes) {
				diffSummNote.toDocument(diffSummaryArray.addObject());
			}
		}

	}

	
	private void init(int refStart, CharSequence refChars, CharSequence queryChars, boolean includeDifferenceSummaryNotes, 
			Map<Integer, List<VariationNote>> refStartToVariationNote) {
		int refPos = refStart;
		char[] diffChars = new char[refChars.length()];
		for(int i = 0; i < refChars.length(); i++) {
			char refChar = refChars.charAt(i);
			char queryChar = queryChars.charAt(i);
			if(refChar == queryChar) {
				diffChars[i] = '-';
			} else {
				diffChars[i] = queryChar;
				if(includeDifferenceSummaryNotes) {
					List<String> variationNames = null;
					List<VariationNote> variationNotes = refStartToVariationNote.get(i+1);
					if(variationNotes != null) {
						variationNames = variationNotes.stream().map(vn -> vn.getVariationName()).collect(Collectors.toList());
					}
					String summaryString = new String(new char[]{refChar}) + Integer.toString(refPos) + new String(new char[]{queryChar});
					differenceSummaryNotes.add(new DifferenceSummaryNote(summaryString, variationNames));
				}
			}
			refPos++;
		}
		this.mask = new String(diffChars);
	}


	public CharSequence getMask() {
		return mask;
	}

	public void setMask(CharSequence maskString) {
		this.mask = maskString;
	}

	@Override
	public void truncateLeft(int length) {
		super.truncateLeft(length);
		setMask(getMask().subSequence(length, getMask().length()));
	}

	@Override
	public void truncateRight(int length) {
		super.truncateRight(length);
		setMask(getMask().subSequence(0, getMask().length() - length));
	}

	public List<DifferenceSummaryNote> getDifferenceSummaryNotes() {
		return differenceSummaryNotes;
	}
	
	
	
	
}
