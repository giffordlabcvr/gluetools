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
package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.Map;
import java.util.Map.Entry;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public class FastaSequenceObject extends AbstractSequenceObject {

	public static final String FASTA_DEFAULT_EXTENSION = "fasta";
	public static final String[] FASTA_ACCEPTED_EXTENSIONS = new String[]{"fasta", "fa", "fna", "fas"};

	
	private String header;
	private String nucleotides;
	
	public FastaSequenceObject() {
		super(SequenceFormat.FASTA);
	}
	
	public FastaSequenceObject(String header, String nucleotides) {
		this();
		this.header = header;
		this.nucleotides = nucleotides.toUpperCase();
	}
	
	@Override
	public String getHeader() {
		return header;
	}

	@Override
	protected String getNucleotidesInternal(CommandContext cmdContext) {
		return nucleotides;
	}

	@Override
	public byte[] toOriginalData() {
		return FastaUtils.seqIdCompoundsPairToFasta(getHeader(), nucleotides, LineFeedStyle.LF).getBytes();
	}

	@Override
	public void fromOriginalData(byte[] originalData) {
		Map<String, DNASequence> seqIdToDna = FastaUtils.parseFasta(originalData);
		if(seqIdToDna.size() == 0) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Zero sequences found in FASTA string");
		}
		if(seqIdToDna.size() > 1) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Multiple sequences found in FASTA string");
		}
		Entry<String, DNASequence> singleEntry = seqIdToDna.entrySet().iterator().next();
		this.header = singleEntry.getKey();
		this.nucleotides = singleEntry.getValue().toString();
	}

}
