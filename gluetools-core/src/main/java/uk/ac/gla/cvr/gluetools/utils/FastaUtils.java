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
package uk.ac.gla.cvr.gluetools.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.translation.ResidueUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.AbstractSequence;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;
import uk.ac.gla.cvr.gluetools.utils.fasta.ProteinSequence;

public class FastaUtils {
	
	public static final String AMINO_ACID_FASTA_DOC_ROOT = "aminoAcidFasta";
	public static final String NUCLEOTIDE_FASTA_DOC_ROOT = "nucleotideFasta";

	public static <T extends AbstractSequence> Map<String, T> parseFasta(byte[] fastaBytes,
			Function<String, T> sequenceCreator, BiFunction<T, String, String> headerParser, boolean trimFastaIdAfterFirstSpace) {
		Map<String, T> idToSequence = new LinkedHashMap<String, T>();
		String fastaString = new String(fastaBytes);
		String[] lines = fastaString.split("\\r|\\r\\n|\\n");
		StringBuffer nextSequenceBuf = null;
		String nextHeaderLine = null;
		for(String line: lines) {
			if(line.trim().length() == 0) {
				continue;
			}
			if(line.startsWith(">")) {
				if(nextHeaderLine != null && nextSequenceBuf != null) {
					T sequence;
					try {
						sequence = sequenceCreator.apply(nextSequenceBuf.toString());
					} catch(FastaUtilsException fue) {
						throw new SequenceException(fue, Code.SEQUENCE_FORMAT_ERROR, "FASTA format error in sequence with header '"+nextHeaderLine+"':"+ fue.getLocalizedMessage());
					}
					String id = headerParser.apply(sequence, nextHeaderLine);
					idToSequence.put(id, sequence);
				}
				nextSequenceBuf = new StringBuffer();
				nextHeaderLine = line.substring(1);
				int firstIndexOfSpace = nextHeaderLine.indexOf(' ');
				if(trimFastaIdAfterFirstSpace && firstIndexOfSpace >= 0) {
					nextHeaderLine = nextHeaderLine.substring(0, firstIndexOfSpace);
				}

			} else {
				if(nextSequenceBuf != null) {
					nextSequenceBuf.append(line.trim());
				} else {
					throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "FASTA format error: First non-whitespace line did not start with '>'");
				}
			}
		}
		// add the final sequence.
		if(nextHeaderLine != null && nextSequenceBuf != null) {
			T sequence;
			try {
				sequence = sequenceCreator.apply(nextSequenceBuf.toString());
			} catch(FastaUtilsException fue) {
				throw new SequenceException(fue, Code.SEQUENCE_FORMAT_ERROR, "FASTA format error in sequence with header '"+nextHeaderLine+"':"+ fue.getLocalizedMessage());
			}
			String id = headerParser.apply(sequence, nextHeaderLine);
			idToSequence.put(id, sequence);
		}
		return idToSequence;
	}
	
	public static DNASequence ntStringToSequence(String ntString) {
		try {
			return new DNASequence(ntString.replace('?', 'N'));
		} catch (FastaUtilsException fue) {
			throw new SequenceException(fue, Code.SEQUENCE_FORMAT_ERROR, "FASTA format error: "+fue.getLocalizedMessage());
		}
	}

	public static ProteinSequence proteinStringToSequence(String proteinString) {
		try {
			return new ProteinSequence(proteinString);
		} catch (FastaUtilsException fue) {
			throw new SequenceException(fue, Code.SEQUENCE_FORMAT_ERROR, "FASTA format error: "+fue.getLocalizedMessage());
		}
	}


	public static Map<String, ProteinSequence> parseFastaProtein(byte[] fastaBytes) {
		return parseFastaProtein(fastaBytes, false);
	}
	
	public static Map<String, ProteinSequence> parseFastaProtein(byte[] fastaBytes, boolean trimFastaIdAfterFirstSpace) {
		return parseFasta(fastaBytes, aaString -> new ProteinSequence(aaString), (ps, line) -> line, trimFastaIdAfterFirstSpace);
	}

	public static Map<String, DNASequence> parseFasta(byte[] fastaBytes) {
		return parseFasta(fastaBytes, false);
	}
	
	public static Map<String, DNASequence> parseFasta(byte[] fastaBytes, boolean trimFastaIdAfterFirstSpace) {
		return parseFasta(fastaBytes, ntString -> new DNASequence(ntString), (ps, line) -> line, true);
	}

	public static byte[] mapToFasta(Map<String, ? extends AbstractSequence> fastaIdToSequence, LineFeedStyle lineFeedStyle) {
		final StringBuffer buf = new StringBuffer();
		fastaIdToSequence.forEach((seqId, abstractSequence) -> 
			buf.append(seqIdCompoundsPairToFasta(seqId, abstractSequence.toString(), lineFeedStyle)));
		return buf.toString().getBytes();
	}
	
	
	public static String seqIdCompoundsPairToFasta(String seqId, String sequenceAsString, LineFeedStyle lineFeedStyle) {
		final StringBuffer buf = new StringBuffer();
		String lb = lineFeedStyle.lineBreakChars;
		buf.append(">").append(seqId).append(lb);
		int start = 0;
		int blockLen = 70;
		while(start + blockLen < sequenceAsString.length()) {
			buf.append(sequenceAsString.substring(start, start+blockLen));
			buf.append(lb);
			start = start+blockLen;
		}
		if(start < sequenceAsString.length()) {
			buf.append(sequenceAsString.substring(start));
			buf.append(lb);
		}
		return buf.toString();
		
	}

	public static char nt(String nucleotides, int position) {
		return nucleotides.charAt(position-1);
	}

	public static int find(String nucleotides, String subSequence, int from) {
		int index = nucleotides.indexOf(subSequence, from-1);
		if(index == -1) { return index; }
		return index+1;
	}

	public static CharSequence subSequence(String nucleotides, int start, int end) {
		return nucleotides.subSequence(start-1, end);
	}

	/**
	 * Given a multi-FASTA, in the form of a map using input keys of type D and a string prefix
	 * @return a multi-FASTA using simple string keys based on the prefix plus an integer, 
	 * populating two maps to map the keys between each other.
	 */
	public static <D, X extends AbstractSequence> Map<String, X> remapFasta(
			Map<D, X> inputKeyToFasta,
			Map<String, D> simpleKeyToInputKey, Map<D, String> inputKeyToSimpleKey, String prefix) {
		int simpleKeyIndex = 0;
		Map<String, X> remappedFasta = new LinkedHashMap<String, X>();
		for(Map.Entry<D, X> entry: inputKeyToFasta.entrySet()) {
			String simpleKey = prefix+simpleKeyIndex;
			D inputKey = entry.getKey();
			GlueLogger.log(Level.FINEST, "Mapped sequence "+inputKey+" as "+simpleKey);
			simpleKeyToInputKey.put(simpleKey, inputKey);
			inputKeyToSimpleKey.put(inputKey, simpleKey);
			X sequence = entry.getValue();
			remappedFasta.put(simpleKey, sequence);
			simpleKeyIndex++;
		}
		return remappedFasta;
	}

	
	public enum LineFeedStyle {
		LF("\n"),
		CRLF("\r\n");
		
		private String lineBreakChars;
		
		private LineFeedStyle(String lineBreakChars) {
			this.lineBreakChars = lineBreakChars;
		}
		
		public String getLineBreakChars() {
			return this.lineBreakChars;
		}
		
		public static LineFeedStyle forOS() {
			if(System.getProperty("os.name").toLowerCase().contains("windows")) {
				return LineFeedStyle.CRLF;
			} else {
				return LineFeedStyle.LF;
			}
			
		}

		
	}
	
	public static String reverseComplement(String nts) {
		int length = nts.length();
		char[] resultChars = new char[length];
		for(int i = 0; i < length; i++) {
			char nt = nts.charAt(i);
			char compNt = ResidueUtils.complementAmbigNtChar(nt);
			resultChars[(length-1) - i] = compNt;
		}
		return new String(resultChars);
		
	}

	public static CommandDocument ntFastaMapToCommandDocument(Map<String, DNASequence> fastaMap) {
		return fastaMapToCommandDocument(fastaMap, NUCLEOTIDE_FASTA_DOC_ROOT);
	}

	public static CommandDocument proteinFastaMapToCommandDocument(Map<String, ProteinSequence> fastaMap) {
		return fastaMapToCommandDocument(fastaMap, AMINO_ACID_FASTA_DOC_ROOT);
	}

	private static CommandDocument fastaMapToCommandDocument(Map<String, ? extends AbstractSequence> fastaIdToSequence, String rootName) {
		CommandDocument commandDocument = new CommandDocument(rootName);
		CommandArray sequenceArray = commandDocument.setArray("sequences");
		fastaIdToSequence.forEach((seqId, abstractSequence) -> {
			CommandObject sequenceObject = sequenceArray.addObject();
			sequenceObject.set("id", seqId);
			sequenceObject.set("sequence", abstractSequence.toString());
		});
		return commandDocument;
	}

	public static Map<String, DNASequence> commandDocumentToNucleotideFastaMap(CommandDocument commandDocument) {
		return commandDocumentToFastaMap(commandDocument, new Function<String, DNASequence>() {
			@Override
			public DNASequence apply(String t) {
				return ntStringToSequence(t);
			}
		}, NUCLEOTIDE_FASTA_DOC_ROOT);
	}

	public static Map<String, ProteinSequence> commandDocumentToProteinFastaMap(CommandDocument commandDocument) {
		return commandDocumentToFastaMap(commandDocument, new Function<String, ProteinSequence>() {
			@Override
			public ProteinSequence apply(String t) {
				return proteinStringToSequence(t);
			}
		}, AMINO_ACID_FASTA_DOC_ROOT);
	}

	private static <S extends AbstractSequence> Map<String, S> commandDocumentToFastaMap(
			CommandDocument commandDocument, Function<String, S> seqParser, String expectedRootName) {
		if(!commandDocument.getRootName().equals(expectedRootName)) {
			throw new FastaUtilsException(FastaUtilsException.Code.FASTA_DOCUMENT_PARSE_ERROR, "Document root name should be '"+expectedRootName+"'");
		}
		Map<String, S> fastaMap = new LinkedHashMap<String, S>();
		CommandArray sequenceArray = Optional.ofNullable(commandDocument.getArray("sequences"))
				.orElse(new CommandArray());
		sequenceArray.getItems().forEach(cmdArrayItem -> {
			if(!(cmdArrayItem instanceof CommandObject)) {
				throw new FastaUtilsException(FastaUtilsException.Code.FASTA_DOCUMENT_PARSE_ERROR, "The 'sequences' array should contain only objects");
			}
			CommandObject sequenceObject = ((CommandObject) cmdArrayItem);
			String id = Optional.ofNullable(sequenceObject.getString("id")).orElseThrow(() -> new FastaUtilsException(FastaUtilsException.Code.FASTA_DOCUMENT_PARSE_ERROR, "Missing 'id' object field"));
			String sequenceString = Optional.ofNullable(sequenceObject.getString("sequence")).orElseThrow(() -> new FastaUtilsException(FastaUtilsException.Code.FASTA_DOCUMENT_PARSE_ERROR, "Missing 'sequence' object field"));
			S sequence = seqParser.apply(sequenceString);
			fastaMap.put(id,  sequence);
		});
		return fastaMap;
	}
	
}
