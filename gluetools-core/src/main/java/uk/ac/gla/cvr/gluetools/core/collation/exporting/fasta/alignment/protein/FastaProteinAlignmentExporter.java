package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractFastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate.OrderStrategy;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaProteinAlignmentExporter")
public class FastaProteinAlignmentExporter extends AbstractFastaAlignmentExporter<FastaProteinAlignmentExporter> {
	
	public FastaProteinAlignmentExporter() {
		super();
		addModulePluginCmdClass(FastaProteinAlignmentExportCommand.class);
	}

	public CommandResult doExport(ConsoleCommandContext cmdContext, String fileName, 
			String alignmentName, Optional<Expression> whereClause, String acRefName, String featureName, 
			Boolean recursive, Boolean preview, OrderStrategy orderStrategy) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = 
				AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		
		int minRefNt = 1;
		int maxRefNt = 1;
		ReferenceSequence acRef = null;
		FeatureLocation featureLoc;
		List<ReferenceSegment> featureRefSegs = null;
		acRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
		featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName));
		featureLoc.getFeature().checkCodesAminoAcids();
		
		int codon1Start = featureLoc.getCodon1Start(cmdContext);
		
		featureRefSegs = featureLoc.segmentsAsReferenceSegments();
		if(!featureRefSegs.isEmpty()) {
			minRefNt = ReferenceSegment.minRefStart(featureRefSegs);
			maxRefNt = ReferenceSegment.maxRefEnd(featureRefSegs);
		}

		Translator translator = new CommandContextTranslator(cmdContext);
		
		Map<Map<String,String>, String> memberPkMapToAlmtRow = new LinkedHashMap<Map<String,String>, String>();
		
		for(AlignmentMember almtMember: almtMembers) {
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			Alignment tipAlmt = almtMember.getAlignment();
			memberQaSegs = tipAlmt.translateToAncConstrainingRef(cmdContext, memberQaSegs, acRef);
			memberQaSegs = ReferenceSegment.intersection(memberQaSegs, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			memberQaSegs = TranslationUtils.truncateToCodonAligned(codon1Start, memberQaSegs);
			
			AbstractSequenceObject seqObj = almtMember.getSequence().getSequenceObject();
			String memberNTs = seqObj.getNucleotides(cmdContext);
			StringBuffer alignmentRow = new StringBuffer();
			int ntIndex = minRefNt;
			for(QueryAlignedSegment seg: memberQaSegs) {
				while(ntIndex < seg.getRefStart()) {
					alignmentRow.append("-");
					ntIndex += 3;
				}
				String segNTs = SegmentUtils.base1SubString(memberNTs, seg.getQueryStart(), seg.getQueryEnd());
				String segAAs = translator.translate(segNTs);
				alignmentRow.append(segAAs);
				ntIndex = seg.getRefEnd()+1;
			}
			while(ntIndex <= maxRefNt) {
				alignmentRow.append("-");
				ntIndex += 3;
			}
			memberPkMapToAlmtRow.put(almtMember.pkMap(), alignmentRow.toString());
		}
		memberPkMapToAlmtRow = orderAlmt(memberPkMapToAlmtRow, orderStrategy);
		
		Map<Map<String,String>, String> memberPkMapToFastaId = new LinkedHashMap<Map<String,String>, String>();
		for(AlignmentMember almtMember: almtMembers) {
			String fastaId = generateFastaId(getIdTemplate(), almtMember);
			memberPkMapToFastaId.put(almtMember.pkMap(), fastaId);
		}
		StringBuffer stringBuffer = new StringBuffer();
		memberPkMapToAlmtRow.forEach((pkMap, almtRow) -> {
			String fastaId = memberPkMapToFastaId.get(pkMap);
			stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, almtRow));
		});
		String fastaString = stringBuffer.toString();
		return formResult(cmdContext, fastaString, fileName, preview);
	}
	
	
	private static Map<Map<String,String>, String> orderAlmt(Map<Map<String,String>, String> alignment, OrderStrategy orderStrategy) {
		if(orderStrategy == null) {
			return alignment;
		}
		ArrayList<SortableAlmtMember> arrayList = 
				new ArrayList<SortableAlmtMember>(
						alignment.entrySet().stream().
						map(entry -> new SortableAlmtMember(entry)).collect(Collectors.toList()));
		Comparator<SortableAlmtMember> comparator = null;
		switch(orderStrategy) {
		case increasing_start_segment:
			comparator = new Comparator<SortableAlmtMember>() {
				@Override
				public int compare(SortableAlmtMember o1, SortableAlmtMember o2) {
					return Integer.compare(computeSortKey(o1), computeSortKey(o2));
				}
				private int computeSortKey(SortableAlmtMember mem) {
					if(mem.sortKey == null) {
						String proteinString = mem.entry.getValue();
						mem.sortKey = -1; 
						for(int i = 0; i < proteinString.length(); i++) {
							if(proteinString.charAt(i) == '-') {
								mem.sortKey = i;
							} else {
								break;
							}
						}
					}
					return mem.sortKey;
				}
			};
		}
		arrayList.sort(comparator);
		Map<Map<String,String>, String> sorted = new LinkedHashMap<Map<String,String>, String>();
		arrayList.forEach(mem -> {
			sorted.put(mem.entry.getKey(), mem.entry.getValue());
		});
		return sorted;
	}

	private static class SortableAlmtMember {
		Map.Entry<Map<String,String>, String> entry;
		Integer sortKey = null;
		public SortableAlmtMember(Entry<Map<String, String>, String> entry) {
			super();
			this.entry = entry;
		}
	}
	

	

}
