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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.AbstractMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaAlignmentExporter",
description="Exports nucleotide alignments to a FASTA file")
public class FastaAlignmentExporter extends AbstractFastaAlignmentExporter<FastaAlignmentExporter> {
	
	public FastaAlignmentExporter() {
		super();
		registerModulePluginCmdClass(FastaAlignmentExportCommand.class);
		registerModulePluginCmdClass(FastaAlignmentWebExportCommand.class);
		
	}



	public static void exportAlignment(
			CommandContext cmdContext, IAlignmentColumnsSelector alignmentColumnsSelector,
			Boolean excludeEmptyRows, 
			AbstractMemberSupplier memberSupplier, AbstractStringAlmtRowConsumer almtRowConsumer) {
		
		List<ReferenceSegment> featureRefSegs = initFeatureRefSegs(cmdContext, alignmentColumnsSelector);
		ReferenceSegment minMaxSeg = initMinMaxSeg(cmdContext, memberSupplier, featureRefSegs);
		
		int numMembers = memberSupplier.countMembers(cmdContext);
		//GlueLogger.getGlueLogger().log(Level.FINEST, "processing "+numMembers+" alignment members");
		int offset = 0;
		//int processed = 0;
		int batchSize = 500;
		while(offset < numMembers) {
			Alignment alignment = memberSupplier.supplyAlignment(cmdContext);
			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
			createAlignment(cmdContext, excludeEmptyRows, alignmentColumnsSelector, alignment, almtMembers, featureRefSegs, minMaxSeg, almtRowConsumer);
			//processed += almtMembers.size();
			//GlueLogger.getGlueLogger().log(Level.FINEST, "processed "+processed+" alignment members");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		
	}

	private static List<ReferenceSegment> initFeatureRefSegs(CommandContext cmdContext, IAlignmentColumnsSelector alignmentColumnsSelector) {
		if(alignmentColumnsSelector != null) {
			return alignmentColumnsSelector.selectAlignmentColumns(cmdContext);
		}
		return null;
	}
	
	private static ReferenceSegment initMinMaxSeg(CommandContext cmdContext, AbstractMemberSupplier memberSupplier, List<ReferenceSegment> featureRefSegs) {
		ReferenceSegment minMaxSeg = new ReferenceSegment(1, 1);
		ReferenceSequence alignmentRef = memberSupplier.supplyAlignment(cmdContext).getRefSequence();
		if(alignmentRef != null) {
			minMaxSeg.setRefEnd(alignmentRef.getSequence().getSequenceObject().getNucleotides(cmdContext).length());
		} else {
			// unconstrained alignment
			int numMembers = memberSupplier.countMembers(cmdContext);
			int offset = 0;
			int batchSize = 500;
			while(offset < numMembers) {
				List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
				for(AlignmentMember almtMember: almtMembers) {
					Integer maxRefEnd = ReferenceSegment.maxRefEnd(almtMember.getAlignedSegments());
					if(maxRefEnd != null) { minMaxSeg.setRefEnd(Math.max(maxRefEnd, minMaxSeg.getRefEnd())); }
				}
				offset += batchSize;
				cmdContext.newObjectContext();
			}
		}
		if(featureRefSegs != null && !featureRefSegs.isEmpty()) {
			minMaxSeg.setRefStart(ReferenceSegment.minRefStart(featureRefSegs));
			minMaxSeg.setRefEnd(ReferenceSegment.maxRefEnd(featureRefSegs));
		}
		return minMaxSeg;
	}
	
	private static void createAlignment(CommandContext cmdContext, Boolean excludeEmptyRows,
			IAlignmentColumnsSelector alignmentColumnsSelector,
			Alignment alignment, List<AlignmentMember> almtMembers, List<ReferenceSegment> featureRefSegs, 
			ReferenceSegment minMaxSeg, AbstractStringAlmtRowConsumer almtRowConsumer) {
		int minRefNt_final = minMaxSeg.getRefStart();
		int maxRefNt_final = minMaxSeg.getRefEnd();

		for(AlignmentMember almtMember: almtMembers) {
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			if(alignmentColumnsSelector != null) {
				// related reference specified in order to specify feature location
				Alignment tipAlmt = almtMember.getAlignment();
				ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, alignmentColumnsSelector.getRelatedRefName());
				memberQaSegs = tipAlmt.translateToRelatedRef(cmdContext, memberQaSegs, relatedRef);
			} else {
				// no feature location but still need to translate to ancestor-constraining reference, 
				// because member is of a descendent alignment.
				if(!alignment.getName().equals(almtMember.getAlignment().getName())) {
					ReferenceSequence acRef2 = alignment.getRefSequence();
					memberQaSegs = almtMember.getAlignment().translateToAncConstrainingRef(cmdContext, memberQaSegs, acRef2);
				}
			}
			if(featureRefSegs != null) {
				memberQaSegs = ReferenceSegment.intersection(memberQaSegs, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			}
			List<QueryAlignedSegment> truncatedQaSegs = ReferenceSegment.intersection(memberQaSegs, Arrays.asList(minMaxSeg), ReferenceSegment.cloneLeftSegMerger());
			String memberNTs = almtMember.getSequence().getSequenceObject().getNucleotides(cmdContext);
			StringBuffer alignmentRow = new StringBuffer(maxRefNt_final);
			int ntIndex = minRefNt_final;
			for(QueryAlignedSegment seg: truncatedQaSegs) {
				while(ntIndex < seg.getRefStart()) {
					alignmentRow.append("-");
					ntIndex++;
				}
				alignmentRow.append(SegmentUtils.base1SubString(memberNTs, seg.getQueryStart(), seg.getQueryEnd()));
				ntIndex = seg.getRefEnd()+1;
			}
			while(ntIndex <= maxRefNt_final) {
				alignmentRow.append("-");
				ntIndex++;
			}
			if( (!truncatedQaSegs.isEmpty()) || !excludeEmptyRows) {
				almtRowConsumer.consumeAlmtRow(cmdContext, almtMember, alignmentRow.toString());
			} 
	    }
	}

	public static Map<Map<String, String>, DNASequence> exportAlignment(
			CommandContext cmdContext,
			IAlignmentColumnsSelector alignmentColumnsSelector,
			boolean excludeEmptyRows, AbstractMemberSupplier memberSupplier) {
		Map<Map<String,String>, DNASequence> pkMapToRowDnaSeq = new LinkedHashMap<Map<String,String>, DNASequence>();
		exportAlignment(cmdContext, alignmentColumnsSelector, excludeEmptyRows, memberSupplier, new AbstractStringAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString) {
				pkMapToRowDnaSeq.put(almtMember.pkMap(), FastaUtils.ntStringToSequence(alignmentRowString));
			}
		});
		return pkMapToRowDnaSeq;
	}
	
	
	
}
