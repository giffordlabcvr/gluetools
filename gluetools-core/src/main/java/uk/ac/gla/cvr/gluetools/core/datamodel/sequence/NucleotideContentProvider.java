package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

public interface NucleotideContentProvider {

	public String getNucleotides(CommandContext cmdContext);
	
	public CharSequence getNucleotides(CommandContext cmdContext, int ntStart, int ntEnd);

	public char nt(CommandContext cmdContext, int position);
}
