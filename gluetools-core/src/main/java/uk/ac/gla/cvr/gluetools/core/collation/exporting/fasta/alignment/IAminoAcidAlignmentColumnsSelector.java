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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.ProteinSequence;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.AbstractMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public interface IAminoAcidAlignmentColumnsSelector extends IAlignmentColumnsSelector {

	// TODO -- logic needs to be moved out of AARegionSelector and into calling commands.
	// which should rely on AARegionSelector.translateQueryNucleotides
	public default void generateStringAlignmentRows(
			CommandContext cmdContext, Boolean excludeEmptyRows,
			AbstractMemberSupplier memberSupplier, AbstractStringAlmtRowConsumer stringAlmtRowConsumer) {
		List<LabeledCodon> selectedLabeledCodons = selectLabeledCodons(cmdContext);
		selectedLabeledCodons.sort(new Comparator<LabeledCodon>() {
			@Override
			public int compare(LabeledCodon o1, LabeledCodon o2) {
				return Integer.compare(o1.getTranscriptionIndex(), o2.getTranscriptionIndex());
			}
			
		});
		TIntIntMap transcriptionIndexToRowStringPos = new TIntIntHashMap(selectedLabeledCodons.size());
		for(int i = 0; i < selectedLabeledCodons.size(); i++) {
			transcriptionIndexToRowStringPos.put(selectedLabeledCodons.get(i).getTranscriptionIndex(), i);
		}
		generateLqaaAlignmentRows(cmdContext, excludeEmptyRows, memberSupplier, new AbstractLqaaAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext,
					AlignmentMember almtMember, List<LabeledQueryAminoAcid> lqaas) {
				char[] rowStringChars = new char[selectedLabeledCodons.size()];
				for(int i = 0; i < rowStringChars.length; i++) {
					rowStringChars[i] = '-';
				}
				for(LabeledQueryAminoAcid lqaa: lqaas) {
					LabeledAminoAcid labeledAminoAcid = lqaa.getLabeledAminoAcid();
					int transcriptionIndex = labeledAminoAcid.getLabeledCodon().getTranscriptionIndex();
					int rowStringPos = transcriptionIndexToRowStringPos.get(transcriptionIndex);
					rowStringChars[rowStringPos] = labeledAminoAcid.getTranslationInfo().getSingleCharTranslation();
				}
				stringAlmtRowConsumer.consumeAlmtRow(cmdContext, almtMember, new String(rowStringChars));
			}
		});
		
	}

	// TODO -- logic needs to be moved out of AARegionSelector and into calling commands.
	// which should rely on AARegionSelector.translateQueryNucleotides
	public default void generateLqaaAlignmentRows(CommandContext cmdContext, Boolean excludeEmptyRows,
			AbstractMemberSupplier memberSupplier, AbstractLqaaAlmtRowConsumer lqaaAlmtRowConsumer) {
		checkAminoAcidSelector(cmdContext);
		int numMembers = memberSupplier.countMembers(cmdContext);
		//GlueLogger.getGlueLogger().log(Level.FINE, "processing "+numMembers+" alignment members");
		int offset = 0;
		//int processed = 0;
		int batchSize = 500;
		while(offset < numMembers) {
			Alignment alignment = memberSupplier.supplyAlignment(cmdContext);
			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
			for(AlignmentMember almtMember: almtMembers) {
				List<LabeledQueryAminoAcid> lqaas = generateAminoAcidAlmtRow(cmdContext, alignment, almtMember);
				if((!excludeEmptyRows) || !lqaas.isEmpty()) {
					lqaaAlmtRowConsumer.consumeAlmtRow(cmdContext, almtMember, lqaas);
				}
			}
			//processed += almtMembers.size();
			//GlueLogger.getGlueLogger().log(Level.FINE, "processed "+processed+" alignment members");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		
	}
	
	public List<LabeledCodon> selectLabeledCodons(CommandContext cmdContext);
	
	public List<LabeledQueryAminoAcid> generateAminoAcidAlmtRow(CommandContext cmdContext, Alignment alignment, AlignmentMember almtMember);

	
	/**
	 * Checks that any features referred to code amino acids.
	 */
	public void checkAminoAcidSelector(CommandContext cmdContext);

	public default Map<Map<String, String>, ProteinSequence> generateAlignmentMap(
			CommandContext cmdContext, Boolean excludeEmptyRows,
			AbstractMemberSupplier memberSupplier) {
		Map<Map<String, String>, ProteinSequence> memberAlignmentMap = new LinkedHashMap<Map<String,String>, ProteinSequence>();
		this.generateStringAlignmentRows(cmdContext, excludeEmptyRows, memberSupplier, new AbstractStringAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext,
					AlignmentMember almtMember, String alignmentRowString) {
				memberAlignmentMap.put(almtMember.pkMap(), FastaUtils.proteinStringToSequence(alignmentRowString));
			}
		});
		return memberAlignmentMap;
	}
	
	public List<LabeledQueryAminoAcid> translateQueryNucleotides(
			CommandContext cmdContext, 
			Translator translator, List<QueryAlignedSegment> queryToRefSegs,
			NucleotideContentProvider queryNucleotideContent);
	
}
