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
package uk.ac.gla.cvr.gluetools.core.translation;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CodonTableUtils.TripletTranslationInfo;

public class TranslationUtils {

	public static TranslationFormat translationFormatFromString(String formatString) {
		try {
			return TranslationFormat.valueOf(formatString);
		} catch(IllegalArgumentException iae) {
			throw new TranslationException(TranslationException.Code.UNKNOWN_TRANSLATION_TYPE, formatString);
		}

	}

	/**
	 * Given a list of segments in NT coordinates, translate them to AA coordinates, given the NT location of codon 1.
	 * 
	 * The returned segments will include codons which are only partially covered by the input segments.
	 * 
	 * However, each codon coordinate will be covered by at most one segment in the returned list.
	 * 
	 */
	
	public static List<ReferenceSegment> translateToCodonCoordinates(int codon1Start, List <? extends IReferenceSegment> ntSegments) {
		List<ReferenceSegment> resultList = new ArrayList<ReferenceSegment>();
		ReferenceSegment lastRefSeg = null;
		for(IReferenceSegment ntSegment: ntSegments) {
			int segmentCodonStart = getCodon(codon1Start, ntSegment.getRefStart());
			int segmentCodonEnd = getCodon(codon1Start, ntSegment.getRefEnd());
			ReferenceSegment newRefSeg = null;
			if(lastRefSeg == null) {
				newRefSeg = new ReferenceSegment(segmentCodonStart, segmentCodonEnd);
			} else if(segmentCodonStart > lastRefSeg.getRefEnd()) {
				newRefSeg = new ReferenceSegment(segmentCodonStart, segmentCodonEnd);
			} else if(segmentCodonEnd > lastRefSeg.getRefEnd()) {
				newRefSeg = new ReferenceSegment(lastRefSeg.getRefEnd()+1, segmentCodonEnd);
			}
			if(newRefSeg != null) {
				resultList.add(newRefSeg);
				lastRefSeg = newRefSeg;
			}
		}
		return resultList;
	}

	/**
	 * Takes a list of NT coordinate IReferenceSegments.
	 * Returns a new list of segments, cloned from the supplied list, such that the new list
	 * contains those parts of the input list segments which contain complete codon-aligned triplets.
	 * 
	 * Example: codon1Start = 9
	 * 
	 * input = [7,12] [20,32]
	 * output = [9,11] [21,32] 
	 * 
	 */
	public static <S extends IReferenceSegment> List<S> truncateToCodonAligned(int codon1Start, List<S> inputSegs) {
		List<S> outputSegs = new ArrayList<S>();
		for(S inputSeg: inputSegs) {
			@SuppressWarnings("unchecked")
			S outputSeg = (S) inputSeg.clone();
			while(outputSeg.getCurrentLength() >= 3 && !isAtStartOfCodon(codon1Start, outputSeg.getRefStart())) {
				outputSeg.truncateLeft(1);
			}
			while(outputSeg.getCurrentLength() >= 3 && !isAtEndOfCodon(codon1Start, outputSeg.getRefEnd())) {
				outputSeg.truncateRight(1);
			}
			if(outputSeg.getCurrentLength() >= 3) {
				outputSegs.add(outputSeg);
			}
		}
		return outputSegs;
	}

	// based on QueryStart.
	public static <S extends IQueryAlignedSegment> List<S> truncateToCodonAlignedQuery(int codon1Start, List<S> inputSegs) {
		List<S> outputSegs = new ArrayList<S>();
		for(S inputSeg: inputSegs) {
			@SuppressWarnings("unchecked")
			S outputSeg = (S) inputSeg.clone();
			while(outputSeg.getCurrentLength() >= 3 && !isAtStartOfCodon(codon1Start, outputSeg.getQueryStart())) {
				outputSeg.truncateLeft(1);
			}
			while(outputSeg.getCurrentLength() >= 3 && !isAtEndOfCodon(codon1Start, outputSeg.getQueryEnd())) {
				outputSeg.truncateRight(1);
			}
			if(outputSeg.getCurrentLength() >= 3) {
				outputSegs.add(outputSeg);
			}
		}
		return outputSegs;
	}

	
	
	/**
	 * Translate possibly ambiguous nucleotides to list of TripletTranslationInfos. 
	 * Assumes nucleotides are in reading frame (i.e. first 3 characters form a codon). 
	 * If the length of input is not a multiple of 3 then trailing nucleotides are discarded.
	 */
	
	public static List<TripletTranslationInfo> translate(CharSequence nts) {
		List<TripletTranslationInfo> translationInfos = new ArrayList<TripletTranslationInfo>();
		char[] codonNts = new char[3];
		for(int ntIndex = 0; ntIndex < nts.length(); ntIndex +=3) {
			if(ntIndex > nts.length() - 3) {
				break;
			}
			for(int i = 0; i < 3; i++) {
				if(ntIndex+i < nts.length()) {
					char nt = nts.charAt(ntIndex+i);
					codonNts[i] = nt;
				}
			}
			translationInfos.add(CodonTableUtils.getTranslationInfo(codonNts));
		}
		return translationInfos;
	}

	public static String translateToAaString(CharSequence nts) {
		List<TripletTranslationInfo> translationInfos = translate(nts);
		return translationInfosToString(translationInfos);
	}

	public static String translationInfosToString(
			List<TripletTranslationInfo> translationInfos) {
		StringBuffer aas = new StringBuffer();
		for(TripletTranslationInfo translationInfo: translationInfos) {
			aas.append(translationInfo.getSingleCharTranslation());
		}
		return aas.toString();
	}
	
	
	public static boolean isNucleotide(char x) {
		return "ACTG".indexOf(x) >= 0;
	}

	public static boolean isAminoAcid(char x) {
		return "FLSY*CWPHQRIMTNKVADEG".indexOf(x) >= 0;
	}


	/**
	 * Given a nucleotide number, which codon is it in.
	 * Example, if codon 1 starts at NT 3285, then NT 3287 is in codon 1, NT 3288 is in codon 2 etc.
	 */
	public static int getCodon(int codon1Start, int ntLocation) {
		return 1 + ((ntLocation - codon1Start) / 3);
	}

	/**
	 * is a nucleotide at the start of a codon.
	 * @param ntLocation
	 * @return
	 */
	public static boolean isAtStartOfCodon(int codon1Start, int ntLocation) {
		return (ntLocation - codon1Start) % 3 == 0;
	}

	/**
	 * is a nucleotide at the start of a codon.
	 * @param ntLocation
	 * @return
	 */
	public static boolean isAtEndOfCodon(int codon1Start, int ntLocation) {
		return (ntLocation - codon1Start) % 3 == 2;
	}

	
	/**
	 * Given a codon number, what nucleotide location does this codon start at.
	 * Example, if codon 1 starts at NT 3285, then codon 4 starts at 3294.
	 */
	public static int getNt(int codon1Start, int codon) {
		return ((codon-1) * 3)+codon1Start;
	}

}
