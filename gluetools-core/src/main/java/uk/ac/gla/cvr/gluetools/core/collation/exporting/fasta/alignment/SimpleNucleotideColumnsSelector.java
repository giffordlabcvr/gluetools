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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelectorException;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.NucleotideRegionSelector;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class SimpleNucleotideColumnsSelector implements IAlignmentColumnsSelector {

	private String relatedRefName;

	private NucleotideRegionSelector nucleotideRegionSelector;
	
	public SimpleNucleotideColumnsSelector(String relatedRefName,
			String featureName, Integer ntStart, Integer ntEnd) {
		super();
		this.relatedRefName = relatedRefName;
		this.nucleotideRegionSelector = new NucleotideRegionSelector();
		this.nucleotideRegionSelector.setFeatureName(featureName);
		this.nucleotideRegionSelector.setStartNt(ntStart);;
		this.nucleotideRegionSelector.setEndNt(ntEnd);;
	}

	@Override
	public List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext) {
		return nucleotideRegionSelector.selectAlignmentColumns(cmdContext, relatedRefName);
	}

	@Override
	public String getRelatedRefName() {
		return relatedRefName;
	}

	@Override
	public void checkAminoAcidSelector(CommandContext cmdContext) {
		throw new AlignmentColumnsSelectorException(AlignmentColumnsSelectorException.Code.INVALID_SELECTOR, 
				"Nucleotide selector may not select amino acid columns");
	}

	
}
