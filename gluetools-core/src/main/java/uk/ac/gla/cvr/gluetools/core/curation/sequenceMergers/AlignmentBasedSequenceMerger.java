package uk.ac.gla.cvr.gluetools.core.curation.sequenceMergers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

/*
 * Module for creating sequences from alignments, based on assigning alignment members into exclusive groups.
 * A new sequence is created for each group, based on the aligned segments of members assigned to the group.
 */
@PluginClass(elemName="alignmentBasedSequenceMerger", 
	description="Creates sequences from alignments, by assigning alignment members to mutually-exclusive groups")
public class AlignmentBasedSequenceMerger extends ModulePlugin<AlignmentBasedSequenceMerger> {

	/*
	 * Generated sequences will be placed within this specified source.
	 */
	public final static String SOURCE_NAME = "sourceName";
	/* 
	 * Freemarker template that will be run on each selected alignment member. 
	 * Members returning the same string result from this template will form a group. 
	 * One sequence will be generated for each such group.
	 */
	public final static String GROUPING_TEMPLATE = "groupingTemplate";
	/* 
	 * Freemarker template that will be run on each selected alignment member within a group. 
	 * Results from this template will be used to sort members within a group.
	 * In the result sequence for the group, segments from members earlier in the ordering 
	 * will have preference over segments from members later in the ordering.
	 * 
	 */
	public final static String SORTING_TEMPLATE = "sortingTemplate";
	/*
	 * For each group, this freemarker template will be run on the sorted list of alignment members assigned to
	 * the group. The resulting string will be used as the sequence ID for squence produced for the group.
	 */
	public final static String SEQUENCE_ID_TEMPLATE = "sequenceIdTemplate";
	
	public AlignmentBasedSequenceMerger() {
		super();
		addSimplePropertyName(SOURCE_NAME);
		addSimplePropertyName(GROUPING_TEMPLATE);
		addSimplePropertyName(SORTING_TEMPLATE);
		addSimplePropertyName(SEQUENCE_ID_TEMPLATE);
		addModulePluginCmdClass(MergeRowsCommand.class);
	}

	private String sourceName;
	private Template groupingTemplate;
	private Template sortingTemplate;
	private Template sequenceIdTemplate;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		this.groupingTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, GROUPING_TEMPLATE, true);
		this.sortingTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, SORTING_TEMPLATE, true);
		this.sequenceIdTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, SEQUENCE_ID_TEMPLATE, true);
	}

	public Map<String, DNASequence> doMergeAlignmentRows(CommandContext cmdContext, List<AlignmentMember> alignmentMembers) {
		List<MemberData> membDatas = new ArrayList<MemberData>();

		for(AlignmentMember almtMemb: alignmentMembers) {
			MemberData membData = new MemberData();
			membData.member = almtMemb;
			membData.templateModel = FreemarkerUtils.templateModelForObject(almtMemb);
			membData.groupingResult = FreemarkerUtils.processTemplate(groupingTemplate, membData.templateModel);
			membData.sortingResult = FreemarkerUtils.processTemplate(sortingTemplate, membData.templateModel);
			membDatas.add(membData);
		}
		
		Map<String, List<MemberData>> groupingValueToMembDatas = membDatas
				.stream()
				.collect(Collectors.groupingBy(md -> md.groupingResult));
			
		Map<String, DNASequence> result = new LinkedHashMap<String, DNASequence>();
		
		groupingValueToMembDatas.values().forEach(groupMembDatas -> {
			groupMembDatas.sort(Comparator.comparing(membData -> membData.sortingResult));
			List<TemplateModel> groupModels = groupMembDatas.stream().map(membData -> membData.templateModel).collect(Collectors.toList());
			Map<String, Object> modelMap = new LinkedHashMap<String, Object>();
			modelMap.put("groupMembers", groupModels);
			TemplateModel groupMembersModel = FreemarkerUtils.templateModelForObject(modelMap);
			String sequenceID = FreemarkerUtils.processTemplate(sequenceIdTemplate, groupMembersModel);
			/* log(Level.FINEST, "Merged sequence sequenceID: "+sequenceID);
			log(Level.FINEST, "Merged sequence members: "+
					groupMembDatas.stream().map(membData -> membData.member.pkMap()).collect(Collectors.toList())); */
			
			List<NtQueryAlignedSegment> ntQaSegs = new ArrayList<NtQueryAlignedSegment>();
			
			for(MemberData membData: groupMembDatas) {
				List<QueryAlignedSegment> membQaSegs = membData.member.segmentsAsQueryAlignedSegments();
				AbstractSequenceObject memberSeqObj = membData.member.getSequence().getSequenceObject();
				
				List<NtQueryAlignedSegment> membNtQaSegs = membQaSegs.stream()
						.map(seg -> new NtQueryAlignedSegment(
								seg.getRefStart(), seg.getRefEnd(), 
								seg.getQueryStart(), seg.getQueryEnd(), 
								memberSeqObj.getNucleotides(cmdContext, seg.getQueryStart(), seg.getQueryEnd())))
						.collect(Collectors.toList());
				
				List<NtQueryAlignedSegment> intersection = ReferenceSegment.intersection(ntQaSegs, membNtQaSegs, ReferenceSegment.cloneLeftSegMerger());
				
				membNtQaSegs = ReferenceSegment.subtract(membNtQaSegs, intersection);
				ntQaSegs.addAll(membNtQaSegs);
				ReferenceSegment.sortByRefStart(ntQaSegs);
			}
			if(ntQaSegs.isEmpty()) {
				log(Level.WARNING, "No segments found for row group with sequenceID "+sequenceID);
				return;
			}
			StringBuffer newSequenceNucleotides = new StringBuffer();
			int lastRefNt = 0;
			for(int i = 0; i < ntQaSegs.size(); i++) {
				NtQueryAlignedSegment ntQaSeg = ntQaSegs.get(i);
				if(i > 0) {
					for(int j = lastRefNt+1; j < ntQaSeg.getRefStart(); j++) {
						newSequenceNucleotides.append("-");
					}
				}
				newSequenceNucleotides.append(ntQaSeg.getNucleotides());
				lastRefNt = ntQaSeg.getRefEnd();
			}
			result.put(sequenceID, FastaUtils.ntStringToSequence(newSequenceNucleotides.toString()));
		});
		return result;
	}
	
	private class MemberData {
		AlignmentMember member;
		TemplateModel templateModel;
		String groupingResult;
		String sortingResult;
	}

	public String getSourceName() {
		return sourceName;
	}
	
}
