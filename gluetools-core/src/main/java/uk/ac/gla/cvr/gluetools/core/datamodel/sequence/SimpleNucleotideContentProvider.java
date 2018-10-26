package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class SimpleNucleotideContentProvider implements NucleotideContentProvider {

	private String nucleotides;
	
	public SimpleNucleotideContentProvider(String nucleotides) {
		super();
		this.nucleotides = nucleotides.toUpperCase();
	}

	@Override
	public String getNucleotides(CommandContext cmdContext) {
		return nucleotides;
	}

	@Override
	public CharSequence getNucleotides(CommandContext cmdContext, int ntStart, int ntEnd) {
		return FastaUtils.subSequence(nucleotides, ntStart, ntEnd);
	}

	@Override
	public char nt(CommandContext cmdContext, int position) {
		return FastaUtils.nt(nucleotides, position);
	}

}
