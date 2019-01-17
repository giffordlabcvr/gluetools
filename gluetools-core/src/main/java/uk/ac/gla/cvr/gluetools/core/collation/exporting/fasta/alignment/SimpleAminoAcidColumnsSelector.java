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

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelectorException;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AminoAcidRegionSelector;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public class SimpleAminoAcidColumnsSelector implements IAminoAcidAlignmentColumnsSelector {

	private String relatedRefName;

	private AminoAcidRegionSelector aaRegionSelector;
	
	public SimpleAminoAcidColumnsSelector(String relatedRefName,
			String featureName, String lcStart, String lcEnd) {
		super();
		this.relatedRefName = relatedRefName;
		this.aaRegionSelector = new AminoAcidRegionSelector();
		this.aaRegionSelector.setFeatureName(featureName);
		this.aaRegionSelector.setStartCodon(lcStart);
		this.aaRegionSelector.setEndCodon(lcEnd);
		
	}

	@Override
	public List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext) {
		return this.aaRegionSelector.selectAlignmentColumns(cmdContext, relatedRefName);
	}

	@Override
	public String getRelatedRefName() {
		return relatedRefName;
	}

	@Override
	public void checkAminoAcidSelector(CommandContext cmdContext) {
		Feature referredToFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(this.aaRegionSelector.getFeatureName()));
		if(!referredToFeature.codesAminoAcids()) {
			throw new AlignmentColumnsSelectorException(AlignmentColumnsSelectorException.Code.INVALID_SELECTOR, 
					"Region selector refers to feature "+referredToFeature.getName()+" which is not an amino acid coding feature");
		}
	}
	
	@Override
	public List<LabeledCodon> selectLabeledCodons(CommandContext cmdContext) {
		return this.aaRegionSelector.selectLabeledCodons(cmdContext, relatedRefName);
	}

	// TODO -- logic needs to be moved out of AARegionSelector and into calling commands.
	// which should rely on AARegionSelector.translateQueryNucleotides
	@Override
	public List<LabeledQueryAminoAcid> generateAminoAcidAlmtRow(
			CommandContext cmdContext,
			List<LabeledCodon> selectedLabeledCodons, Alignment alignment,
			AlignmentMember almtMember) {
		ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, getRelatedRefName());
		Translator translator = new CommandContextTranslator(cmdContext);
		return this.aaRegionSelector
					.generateAminoAcidAlmtRow(cmdContext, relatedRef, translator, selectedLabeledCodons, almtMember);
	}

	@Override
	public List<LabeledQueryAminoAcid> translateQueryNucleotides(CommandContext cmdContext,
			List<QueryAlignedSegment> queryToRefSegs, NucleotideContentProvider queryNucleotideContent) {
		Translator translator = new CommandContextTranslator(cmdContext);
		return this.aaRegionSelector.translateQueryNucleotides(cmdContext, getRelatedRefName(), queryToRefSegs, translator, queryNucleotideContent);
	}

	
}
