package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.ExplicitMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaNtAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.mafft.MafftRunner;
import uk.ac.gla.cvr.gluetools.programs.mafft.MafftRunner.Task;
import uk.ac.gla.cvr.gluetools.programs.mafft.add.MafftResult;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;


@PluginClass(elemName="mafftAligner",
		description="Runs external MAFFT program to populate or extend an unconstrained Alignment")
public class MafftAligner extends Aligner<MafftAligner.MafftAlignerResult, MafftAligner> 
	implements SupportsExtendUnconstrained<MafftAligner.MafftAlignerResult>, SupportsComputeUnconstrained {

	public static final String ALIGNMENT_REIMPORTER_MODULE_NAME = "alignmentReimporterModuleName";
	
	private MafftRunner mafftRunner = new MafftRunner();
	
	// module to reimport the alignment after it's been processed by MAFFT.
	private String alignmentReimporterModuleName;
	
	public MafftAligner() {
		super();
		addSimplePropertyName(ALIGNMENT_REIMPORTER_MODULE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignmentReimporterModuleName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_REIMPORTER_MODULE_NAME, true);
		Element mafftRunnerElem = PluginUtils.findConfigElement(configElem, "mafftRunner");
		if(mafftRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, mafftRunnerElem, mafftRunner);
		}
	}

	
	public static class MafftAlignerResult extends Aligner.AlignerResult {
		public MafftAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("mafftAlignerResult", fastaIdToAlignedSegments);
		}
	}


	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		FastaNtAlignmentImporter<?> alignmentReimporter = resolveReimporter(cmdContext);
		alignmentReimporter.validate(cmdContext);
	}

	private FastaNtAlignmentImporter<?> resolveReimporter(
			CommandContext cmdContext) {
		FastaNtAlignmentImporter<?> alignmentReimporter = 
				Module.resolveModulePlugin(cmdContext, FastaNtAlignmentImporter.class, alignmentReimporterModuleName);
		return alignmentReimporter;
	}

	@Override
	public Map<Map<String, String>, List<QueryAlignedSegment>> extendUnconstrained(
			CommandContext cmdContext, 
			String alignmentName,
			List<Map<String, String>> existingMembersPkMaps,
			List<Map<String, String>> recomputedMembersPkMaps, 
			File dataDir) {
		FastaNtAlignmentImporter<?> alignmentReimporter = resolveReimporter(cmdContext);
		int existingIdx = 0;
		Map<String, Map<String,String>> existingTempIdToPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<String, DNASequence> existingTempIdToSequence = new LinkedHashMap<String, DNASequence>();
		
		ExplicitMemberSupplier explicitMemberSupplier = new ExplicitMemberSupplier(alignmentName, existingMembersPkMaps);
		
		Map<Map<String, String>, DNASequence> existingPkMapToSequence = 
				FastaAlignmentExporter.exportAlignment(cmdContext, null, 
						false, explicitMemberSupplier);
		
		for(Map<String,String> pkMap: existingMembersPkMaps) {
			String tempId = "E"+existingIdx;
			existingTempIdToPkMap.put(tempId, pkMap);
			existingTempIdToSequence.put(tempId, existingPkMapToSequence.get(pkMap));
			existingIdx++;
		};
		
		int recomputedIdx = 0;
		Map<String, Map<String,String>> recomputedTempIdToPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<String, DNASequence> recomputedTempIdToSequence = new LinkedHashMap<String, DNASequence>();
		for(Map<String,String> pkMap: recomputedMembersPkMaps) {
			AlignmentMember recomputedMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, pkMap);
			DNASequence sequence = FastaUtils.ntStringToSequence(recomputedMember.getSequence().getSequenceObject().getNucleotides(cmdContext));
			String tempId = "R"+recomputedIdx;
			recomputedTempIdToPkMap.put(tempId, pkMap);
			recomputedTempIdToSequence.put(tempId, sequence);
			recomputedIdx++;
		};
		MafftResult mafftResult = mafftRunner.executeMafft(cmdContext, Task.ADD_KEEPLENGTH, true, existingTempIdToSequence, recomputedTempIdToSequence, dataDir);
		Map<String, DNASequence> alignmentWithQuery = mafftResult.getResultAlignment();
		Map<Map<String,String>, List<QueryAlignedSegment>> pkMapToSegs = new LinkedHashMap<Map<String,String>, List<QueryAlignedSegment>>();
		alignmentWithQuery.forEach((tempID, dnaSequence) -> {
			Map<String, String> pkMap = existingTempIdToPkMap.get(tempID);
			if(pkMap == null) {
				pkMap = recomputedTempIdToPkMap.get(tempID);
			}
			AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, pkMap);
			Sequence foundSequence = almtMember.getSequence();
			List<QueryAlignedSegment> existingSegs = new ArrayList<QueryAlignedSegment>();
			List<QueryAlignedSegment> newSegs = alignmentReimporter.findAlignedSegs(cmdContext, foundSequence, existingSegs, dnaSequence.getSequenceAsString());
			pkMapToSegs.put(pkMap, newSegs);
		});
		return pkMapToSegs;
	}

	@Override
	public Map<Map<String, String>, List<QueryAlignedSegment>> computeUnconstrained(
			CommandContext cmdContext, String alignmentName) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> members = AlignmentListMemberCommand.listMembers(cmdContext, alignment, false, Optional.empty());
		List<Map<String,String>> memberPkMaps = members.stream().map(memb -> memb.pkMap()).collect(Collectors.toList());

		FastaNtAlignmentImporter<?> alignmentReimporter = resolveReimporter(cmdContext);

		int memberIdx = 0;
		Map<String, Map<String,String>> memberTempIdToPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<String, DNASequence> memberTempIdToSequence = new LinkedHashMap<String, DNASequence>();
		
		Map<Map<String, String>, DNASequence> memberPkMapToSequence = new LinkedHashMap<Map<String,String>, DNASequence>(); 

		members.forEach(member -> {
			DNASequence dnaSequence = FastaUtils.ntStringToSequence(member.getSequence().getSequenceObject().getNucleotides(cmdContext));
			memberPkMapToSequence.put(member.pkMap(), dnaSequence);
		});
			
		for(Map<String,String> memberPkMap: memberPkMaps) {
			String tempId = "M"+memberIdx;
			memberTempIdToPkMap.put(tempId, memberPkMap);
			memberTempIdToSequence.put(tempId, memberPkMapToSequence.get(memberPkMap));
			memberIdx++;
		};
		MafftResult mafftResult = mafftRunner.executeMafft(cmdContext, Task.COMPUTE, false, memberTempIdToSequence, null, null);
		Map<String, DNASequence> alignmentWithQuery = mafftResult.getResultAlignment();
		Map<Map<String,String>, List<QueryAlignedSegment>> pkMapToSegs = new LinkedHashMap<Map<String,String>, List<QueryAlignedSegment>>();
		alignmentWithQuery.forEach((tempID, dnaSequence) -> {
			Map<String, String> pkMap = memberTempIdToPkMap.get(tempID);
			AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, pkMap);
			Sequence foundSequence = almtMember.getSequence();
			List<QueryAlignedSegment> existingSegs = new ArrayList<QueryAlignedSegment>();
			List<QueryAlignedSegment> newSegs = alignmentReimporter.findAlignedSegs(cmdContext, foundSequence, existingSegs, dnaSequence.getSequenceAsString());
			pkMapToSegs.put(pkMap, newSegs);
		});
		return pkMapToSegs;
	}

	
}
