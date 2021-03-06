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
package uk.ac.gla.cvr.gluetools.core.curation.aligners.mafft;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.ExplicitMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.BaseFastaAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeConstrained;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeUnconstrained;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsExtendUnconstrained;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
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
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;


@PluginClass(elemName="mafftAligner",
		description="Runs external MAFFT program to populate or extend an unconstrained Alignment")
public class MafftAligner extends Aligner<MafftAligner.MafftAlignerResult, MafftAligner> 
	implements SupportsExtendUnconstrained<MafftAligner.MafftAlignerResult>, SupportsComputeUnconstrained, SupportsComputeConstrained {

	public static final String ALIGNMENT_REIMPORTER_MODULE_NAME = "alignmentReimporterModuleName";
	
	private MafftRunner mafftRunner = new MafftRunner();
	
	// module to reimport the alignment after it's been processed by MAFFT.
	private String alignmentReimporterModuleName;
	
	public MafftAligner() {
		super();
		addSimplePropertyName(ALIGNMENT_REIMPORTER_MODULE_NAME);
		registerModulePluginCmdClass(MafftAlignerAlignCommand.class);
		registerModulePluginCmdClass(MafftAlignerFileAlignCommand.class);

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
		BaseFastaAlignmentImporter<?> alignmentReimporter = resolveReimporter(cmdContext);
		alignmentReimporter.validate(cmdContext);
	}

	private BaseFastaAlignmentImporter<?> resolveReimporter(
			CommandContext cmdContext) {
		BaseFastaAlignmentImporter<?> alignmentReimporter = 
				Module.resolveModulePlugin(cmdContext, BaseFastaAlignmentImporter.class, alignmentReimporterModuleName);
		return alignmentReimporter;
	}

	@Override
	public Map<Map<String, String>, List<QueryAlignedSegment>> extendUnconstrained(
			CommandContext cmdContext, 
			String alignmentName,
			List<Map<String, String>> existingMembersPkMaps,
			List<Map<String, String>> recomputedMembersPkMaps, 
			File dataDir) {
		BaseFastaAlignmentImporter<?> alignmentReimporter = resolveReimporter(cmdContext);
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
			String queryId = foundSequence.getSource().getName()+"/"+foundSequence.getSequenceID();
			String queryNucleotides = foundSequence.getSequenceObject().getNucleotides(cmdContext);
			List<QueryAlignedSegment> newSegs = 
					alignmentReimporter.alignmentRowImport(cmdContext, queryId, queryNucleotides, dnaSequence.getSequenceAsString());
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

		BaseFastaAlignmentImporter<?> alignmentReimporter = resolveReimporter(cmdContext);

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
			String queryId = foundSequence.getSource().getName()+"/"+foundSequence.getSequenceID();
			String queryNucleotides = foundSequence.getSequenceObject().getNucleotides(cmdContext);
			List<QueryAlignedSegment> newSegs = 
					alignmentReimporter.alignmentRowImport(cmdContext, queryId, queryNucleotides, dnaSequence.getSequenceAsString());
			pkMapToSegs.put(pkMap, newSegs);
		});
		return pkMapToSegs;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends AlignCommand> getComputeConstrainedCommandClass() {
		return MafftAlignerAlignCommand.class;
	}

	@Override
	public MafftAlignerResult computeConstrained(CommandContext cmdContext, String refName,
			Map<String, DNASequence> queryIdToNucleotides) {
		
		Map<String, List<QueryAlignedSegment>> constrainedQueryIdToAlignedSegments = 
				new LinkedHashMap<String, List<QueryAlignedSegment>>();

		ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refName));
		String refNucleotides = refSeq.getSequence().getSequenceObject().getNucleotides(cmdContext);

		queryIdToNucleotides.forEach( (queryId, queryDnaSequence) -> {

			Map<String, DNASequence> mafftInput = new LinkedHashMap<String, DNASequence>();
			DNASequence referenceDnaSequence = new DNASequence(refNucleotides);
			mafftInput.put("R", referenceDnaSequence);
			mafftInput.put("Q", queryDnaSequence);

			MafftResult mafftResult = mafftRunner.executeMafft(cmdContext, Task.COMPUTE, false, mafftInput, null, null);
			Map<String, DNASequence> resultAlignment = mafftResult.getResultAlignment();
			
			BaseFastaAlignmentImporter<?> alignmentReimporter = resolveReimporter(cmdContext);

			List<QueryAlignedSegment> queryToUQaSegs = alignmentReimporter
					  .alignmentRowImport(cmdContext, queryId, queryDnaSequence.getSequenceAsString(), 
							  resultAlignment.get("Q").getSequenceAsString());
			List<QueryAlignedSegment> referenceToUQaSegs = alignmentReimporter
					  .alignmentRowImport(cmdContext, refName, referenceDnaSequence.getSequenceAsString(), 
							  resultAlignment.get("R").getSequenceAsString());

			// because Task.COMPUTE was used, this result alignment will be unconstrained.
			// we now just need to convert it into a constrained alignment
			List<QueryAlignedSegment> uToReferenceQaSegs = QueryAlignedSegment.invertList(referenceToUQaSegs);
			List<QueryAlignedSegment> queryToRefQaSegs = QueryAlignedSegment.translateSegments(queryToUQaSegs, uToReferenceQaSegs);
			
			constrainedQueryIdToAlignedSegments.put(queryId, queryToRefQaSegs);
		});

		
		return new MafftAlignerResult(constrainedQueryIdToAlignedSegments);

		
	}

}
