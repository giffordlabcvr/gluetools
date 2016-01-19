package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import jline.internal.InputStreamReader;

import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class Sam2ConsensusSequenceObject extends AbstractSequenceObject 
	implements SupportsMinorityVariants<Sam2ConsensusSequenceObject, Sam2ConsensusMinorityVariantFilter>{

	public Sam2ConsensusSequenceObject() {
		super(SequenceFormat.SAM2CONSENSUS_EXTENDED);
	}

	private String header;
	private String nucleotides;
	private NtLocation[] ntLocations;

	public enum LocationField {
		consensus,
		a,
		t,
		g,
		c,
		n,
		aFreq,
		tFreq,
		gFreq,
		cFreq,
		nFreq,
		aQual,
		tQual,
		gQual,
		cQual,
		coverage,
		entropy,
	}
	
	public static class NtLocation {
		private Object[] values = new Object[LocationField.values().length];
		public void set(LocationField field, Object value) {
			values[field.ordinal()] = value;
		}
		public Object get(LocationField field) {
			return values[field.ordinal()];
		}
	}

	@SuppressWarnings("resource")
	@Override
	public void fromOriginalData(byte[] originalData) {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(originalData)));
		LinkedList<String> lines = new LinkedList<String>(bufferedReader.lines().collect(Collectors.toList()));
		if(lines.isEmpty()) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "No header line");
		}
		String headerLine = lines.removeFirst();
		if(!headerLine.startsWith(">")) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "First line did not start with '>'");
		}
		this.header = headerLine.replaceFirst(">", "").trim();
		if(this.header.length() == 0) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Empty header");
		}
		String line = null;
		StringBuffer ntBuf = new StringBuffer();
		boolean columnHeadersLineFound = false;
		while(!lines.isEmpty()) {
			line = lines.removeFirst();
			if(line.startsWith("Position")) {
				columnHeadersLineFound = true;
				break;
			} else {
				ntBuf.append(line.trim());
			}
		}
		this.nucleotides = ntBuf.toString();
		if(nucleotides.isEmpty()) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "No nucleotides found");
		}
		try {
			new DNASequence(nucleotides, AmbiguityDNACompoundSet.getDNACompoundSet());
		} catch (CompoundNotFoundException e) {
			throw new SequenceException(e, Code.SEQUENCE_FORMAT_ERROR, "Nucleotides error: "+e.getLocalizedMessage());
		}
		if(!columnHeadersLineFound) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "No column headers found");
		}
		String[] columnHeaders = line.split(",");
		String[] expectedColumnHeaders = { "Position", "Consensus", "A", "T",
				"G", "C", "N", "", "A Freq", "T Freq", "G Freq", "C Freq", "N Freq", "",
				"A Qual", "T Qual", "G Qual", "C Qual", "", "Coverage", "",
				"Entropy"};
		if (columnHeaders.length != expectedColumnHeaders.length) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Expected "+expectedColumnHeaders.length+" column headers, found "+columnHeaders.length);
		}
		for(int i = 0; i < columnHeaders.length; i++) {
			if(!columnHeaders[i].equals(expectedColumnHeaders[i])) {
				throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Expected column header "+expectedColumnHeaders[i]+", found "+columnHeaders[i]);
			}
		}
		int expectedPosition = 0;
		List<NtLocation> ntLocations = new ArrayList<NtLocation>();
		while(!lines.isEmpty()) {
			expectedPosition++;
			line = lines.removeFirst();
			String[] cells = line.split(",");
			if(cells.length != columnHeaders.length) {
				throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Position "+expectedPosition+", expected "+columnHeaders.length+" cells, found "+cells.length);
			}
			int position = getInt(cells, columnHeaders, 0, expectedPosition);
			if(position != expectedPosition) {
				throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Expected position "+expectedPosition+", found "+position);
			}
			NtLocation ntLocation = new NtLocation();
			ntLocation.set(LocationField.consensus, getChar(cells, expectedColumnHeaders, 1, expectedPosition));
			ntLocation.set(LocationField.a, getInt(cells, expectedColumnHeaders, 2, expectedPosition));
			ntLocation.set(LocationField.t, getInt(cells, expectedColumnHeaders, 3, expectedPosition));
			ntLocation.set(LocationField.g, getInt(cells, expectedColumnHeaders, 4, expectedPosition));
			ntLocation.set(LocationField.c, getInt(cells, expectedColumnHeaders, 5, expectedPosition));
			ntLocation.set(LocationField.n, getInt(cells, expectedColumnHeaders, 6, expectedPosition));
			ntLocation.set(LocationField.aFreq, getInt(cells, expectedColumnHeaders, 8, expectedPosition));
			ntLocation.set(LocationField.tFreq, getInt(cells, expectedColumnHeaders, 9, expectedPosition));
			ntLocation.set(LocationField.gFreq, getInt(cells, expectedColumnHeaders, 10, expectedPosition));
			ntLocation.set(LocationField.cFreq, getInt(cells, expectedColumnHeaders, 11, expectedPosition));
			ntLocation.set(LocationField.nFreq, getInt(cells, expectedColumnHeaders, 12, expectedPosition));
			ntLocation.set(LocationField.aQual, getInt(cells, expectedColumnHeaders, 14, expectedPosition));
			ntLocation.set(LocationField.tQual, getInt(cells, expectedColumnHeaders, 15, expectedPosition));
			ntLocation.set(LocationField.gQual, getInt(cells, expectedColumnHeaders, 16, expectedPosition));
			ntLocation.set(LocationField.cQual, getInt(cells, expectedColumnHeaders, 17, expectedPosition));
			ntLocation.set(LocationField.coverage, getInt(cells, expectedColumnHeaders, 19, expectedPosition));
			ntLocation.set(LocationField.entropy, getDouble(cells, expectedColumnHeaders, 21, expectedPosition));
			ntLocations.add(ntLocation);
		}
		if(ntLocations.size() != nucleotides.length()) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Expected "+nucleotides.length()+" position lines but found "+ntLocations.size());
		}
		this.ntLocations = ntLocations.toArray(new NtLocation[]{});
	}

	private double getDouble(String[] cells, String[] columnHeaders, int columnIndex, int expectedPosition) {
		try {
			return Double.parseDouble(cells[columnIndex]);
		} catch(NumberFormatException nfe) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Position "+expectedPosition+", column "+
					columnHeaders[columnIndex]+", expected floating point number, found: \""+cells[columnIndex]+"\"");
		}
	}

	private int getInt(String[] cells, String[] columnHeaders, int columnIndex, int expectedPosition) {
		try {
			return Integer.parseInt(cells[columnIndex]);
		} catch(NumberFormatException nfe) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Position "+expectedPosition+", column "+
					columnHeaders[columnIndex]+", expected integer, found: \""+cells[columnIndex]+"\"");
		}
	}

	private Character getChar(String[] cells, String[] columnHeaders, int columnIndex, int expectedPosition) {
		if(cells[columnIndex].length() != 1) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Position "+expectedPosition+", column "+
					columnHeaders[columnIndex]+", expected single character, found: \""+cells[columnIndex]+"\"");
		}
		return cells[columnIndex].charAt(0);
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	protected String getNucleotides() {
		return nucleotides;
	}

	public Object getLocationFieldValue(int position, LocationField field) {
		return ntLocations[position-1].get(field);
	}
	
	public double getEntropy(int position) {
		return ((Double) getLocationFieldValue(position, LocationField.entropy)).doubleValue();
	}

	public char getConsensus(int position) {
		return ((Character) getLocationFieldValue(position, LocationField.consensus)).charValue();
	}

	public double getProportion(int position, char ntValue) {
		LocationField ntCountField;
		switch(ntValue) {
		case 'A':
			ntCountField = LocationField.a;
			break;
		case 'T':
			ntCountField = LocationField.t;
			break;
		case 'C':
			ntCountField = LocationField.c;
			break;
		case 'G':
			ntCountField = LocationField.g;
			break;
		default:
			throw new RuntimeException("Bad NT value: "+ntValue);
		}
		int ntCount = ((Integer) getLocationFieldValue(position, ntCountField)).intValue();
		int total = ((Integer) getLocationFieldValue(position, LocationField.coverage)).intValue();
		return ((double) ntCount) / total;
	}

	
	public int getQuality(int position, char ntValue) {
		LocationField ntQualityField;
		switch(ntValue) {
		case 'A':
			ntQualityField = LocationField.aQual;
			break;
		case 'T':
			ntQualityField = LocationField.tQual;
			break;
		case 'C':
			ntQualityField = LocationField.cQual;
			break;
		case 'G':
			ntQualityField = LocationField.gQual;
			break;
		default:
			throw new RuntimeException("Bad NT value: "+ntValue);
		}
		return ((Integer) getLocationFieldValue(position, ntQualityField)).intValue();
	}

	

	@Override
	public byte[] toOriginalData() {
		StringBuffer buf = new StringBuffer();
		buf.append(FastaUtils.seqIdCompoundsPairToFasta(header, nucleotides));
		buf.append("Position,Consensus,A,T,G,C,N,,A Freq,T Freq,G Freq,C Freq,N Freq,,A Qual,T Qual,G Qual,C Qual,,Coverage,,Entropy\n");
		for(int i = 0; i < ntLocations.length; i++) {
			buf.append(i+1).append(",");
			buf.append(ntLocations[i].get(LocationField.consensus)).append(",");
			buf.append(ntLocations[i].get(LocationField.a)).append(",");
			buf.append(ntLocations[i].get(LocationField.t)).append(",");
			buf.append(ntLocations[i].get(LocationField.g)).append(",");
			buf.append(ntLocations[i].get(LocationField.c)).append(",");
			buf.append(ntLocations[i].get(LocationField.n)).append(",,");
			buf.append(ntLocations[i].get(LocationField.aFreq)).append(",");
			buf.append(ntLocations[i].get(LocationField.tFreq)).append(",");
			buf.append(ntLocations[i].get(LocationField.gFreq)).append(",");
			buf.append(ntLocations[i].get(LocationField.cFreq)).append(",");
			buf.append(ntLocations[i].get(LocationField.nFreq)).append(",,");
			buf.append(ntLocations[i].get(LocationField.aQual)).append(",");
			buf.append(ntLocations[i].get(LocationField.tQual)).append(",");
			buf.append(ntLocations[i].get(LocationField.gQual)).append(",");
			buf.append(ntLocations[i].get(LocationField.cQual)).append(",,");
			buf.append(ntLocations[i].get(LocationField.coverage)).append(",,");
			buf.append(ntLocations[i].get(LocationField.entropy)).append("\n");
		}
		return buf.toString().getBytes();
	}

}
