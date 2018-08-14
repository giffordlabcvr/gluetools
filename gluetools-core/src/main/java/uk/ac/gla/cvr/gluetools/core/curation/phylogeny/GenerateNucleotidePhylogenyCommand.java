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
package uk.ac.gla.cvr.gluetools.core.curation.phylogeny;

import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleNucleotideColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;

public abstract class GenerateNucleotidePhylogenyCommand<P extends PhylogenyGenerator<P>> extends GeneratePhylogenyCommand<P> {

	@Override
	protected final OkResult execute(CommandContext cmdContext, P modulePlugin) {
		IAlignmentColumnsSelector alignmentColumnsSelector = resolveSelector(cmdContext);
		QueryMemberSupplier queryMemberSupplier = resolveQueryMemberSupplier();
		
		Map<Map<String, String>, DNASequence> memberNucleotideAlignment = 
				FastaAlignmentExporter.exportAlignment(cmdContext, alignmentColumnsSelector, false, queryMemberSupplier);
		
		PhyloTree phyloTree = generateNucleotidePhylogeny(cmdContext, modulePlugin, memberNucleotideAlignment);

		saveTree(cmdContext, phyloTree);

		return new OkResult();
	}

	
	private IAlignmentColumnsSelector resolveSelector(CommandContext cmdContext) {
		IAlignmentColumnsSelector alignmentColumnsSelector;
		
		if(getSelectorName() != null) {
			alignmentColumnsSelector = Module.resolveModulePlugin(cmdContext, AlignmentColumnsSelector.class, getSelectorName());
		} else if(getRelRefName() != null && getFeatureName() != null) {
			alignmentColumnsSelector = new SimpleNucleotideColumnsSelector(getRelRefName(), getFeatureName(), null, null);
		} else {
			alignmentColumnsSelector = null;
		}
		return alignmentColumnsSelector;
	}

	
	protected abstract PhyloTree generateNucleotidePhylogeny(CommandContext cmdContext, P modulePlugin, Map<Map<String, String>, DNASequence> memberNucleotideAlignment);


}
