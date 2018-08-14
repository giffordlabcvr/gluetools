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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.biojava.nbio.core.sequence.ProteinSequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.AbstractMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public interface IAminoAcidAlignmentColumnsSelector extends IAlignmentColumnsSelector {

	public default void generateAlignmentRows(
			CommandContext cmdContext, Boolean excludeEmptyRows,
			AbstractMemberSupplier memberSupplier, AbstractAlmtRowConsumer almtRowConsumer) {
		checkCoding(cmdContext);
		int numMembers = memberSupplier.countMembers(cmdContext);
		GlueLogger.getGlueLogger().log(Level.FINE, "processing "+numMembers+" alignment members");
		int offset = 0;
		int processed = 0;
		int batchSize = 500;
		List<ReferenceSegment> featureRefSegs = selectAlignmentColumns(cmdContext);
		ReferenceSegment minMaxSeg = IAminoAcidAlignmentColumnsSelector.initMinMaxSeg(featureRefSegs);
		while(offset < numMembers) {
			Alignment alignment = memberSupplier.supplyAlignment(cmdContext);

			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);

			for(AlignmentMember almtMember: almtMembers) {
				String almtRowString = generateAminoAcidAlmtRowString(cmdContext, featureRefSegs, minMaxSeg, alignment, almtMember);
				if((!excludeEmptyRows) || almtRowString.matches("-*")) {
					almtRowConsumer.consumeAlmtRow(cmdContext, almtMember, almtRowString);
				}
			}
			processed += almtMembers.size();
			GlueLogger.getGlueLogger().log(Level.FINE, "processed "+processed+" alignment members");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
	}

	public String generateAminoAcidAlmtRowString(CommandContext cmdContext, List<ReferenceSegment> featureRefSegs, ReferenceSegment minMaxSeg, Alignment alignment, AlignmentMember almtMember);

	public static ReferenceSegment initMinMaxSeg(
			List<ReferenceSegment> featureRefSegs) {
		int minRefNt = 1;
		int maxRefNt = 1;
		if(!featureRefSegs.isEmpty()) {
			minRefNt = ReferenceSegment.minRefStart(featureRefSegs);
			maxRefNt = ReferenceSegment.maxRefEnd(featureRefSegs);
		}
		ReferenceSegment minMaxSeg = new ReferenceSegment(minRefNt, maxRefNt);
		return minMaxSeg;
	}

	
	
	/**
	 * Checks that any features referred to code amino acids.
	 */
	public void checkCoding(CommandContext cmdContext);

	public default Map<Map<String, String>, ProteinSequence> generateAlignmentMap(
			CommandContext cmdContext, Boolean excludeEmptyRows,
			AbstractMemberSupplier memberSupplier) {
		Map<Map<String, String>, ProteinSequence> memberAlignmentMap = new LinkedHashMap<Map<String,String>, ProteinSequence>();
		this.generateAlignmentRows(cmdContext, excludeEmptyRows, memberSupplier, new AbstractAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext,
					AlignmentMember almtMember, String alignmentRowString) {
				memberAlignmentMap.put(almtMember.pkMap(), FastaUtils.proteinStringToSequence(alignmentRowString));
			}
		});
		return memberAlignmentMap;
	}
	
}
