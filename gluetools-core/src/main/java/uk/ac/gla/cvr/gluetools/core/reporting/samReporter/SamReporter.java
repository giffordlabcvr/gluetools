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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylogenyImporter.PhyloImporter;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer.PlacerResultInternal;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodSinglePlacement;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodSingleQueryResult;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.PlacementNeighbour;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.PlacementNeighbourFinder;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterCommandException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@PluginClass(elemName="samReporter",
		description="Provides various commands for analysing deep sequencing data in SAM/BAM files")
public class SamReporter extends ModulePlugin<SamReporter> {

	public static final String MAX_LIKELIHOOD_PLACER_MODULE_NAME = "maxLikelihoodPlacerModuleName";
	public static final String MAX_LIKELIHOOD_PLACER_DISTANCE_CUTOFF = "maxLikelihoodPlacerDistanceCutoff";
	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	public static final String READ_LOG_INTERVAL = "readLogInterval";
	public static final String SAM_READER_VALIDATION_STRINGENCY = "samReaderValidationStringency";
	public static final String DEFAULT_MIN_Q_SCORE = "defaultMinQScore";
	public static final String DEFAULT_MIN_DEPTH = "defaultMinDepth";
	public static final String DEFAULT_MIN_MAP_Q = "defaultMinMapQ";
	public static final String DEFAULT_SAM_REF_SENSE = "defaultSamRefSense";
	
	public enum SamRefSense {
		FORWARD, // assume SAM reference is in the same sense as the GLUE reference
		REVERSE_COMPLEMENT, // assume SAM reference is reverse complement relative to GLUE reference
	}

	
	// Maximum likelihood placer module: in some cases selects target ref by doing a placement of the consensus sequence.
	private String maxLikelihoodPlacerModuleName;
	// cutoff for evo distance to nearest reference if ML-placer is used
	private Double maxLikelihoodPlacerDistanceCutoff;
	
	
	// Aligner module: generates pairwise alignment between sam reference and target ref.
	private String alignerModuleName;
	private Integer readLogInterval;
	// STRICT (default), LENIENT, or SILENT
	private ValidationStringency samReaderValidationStringency;
	
	// minimum quality score used by commands, if no minimum quality score is supplied when the command is executed
	private int defaultMinQScore;

	// minimum mapping quality used by commands, if no minimum mapping quality is supplied when the command is executed
	private int defaultMinMapQ;

	
	// minimum read depth used by commands, if no minimum read depth is supplied when the command is executed
	private int defaultMinDepth;
	
	// samRefSense used by commands if no samRefSense is supplied.
	private SamRefSense defaultSamRefSense;
	
	public SamReporter() {
		super();
		registerModulePluginCmdClass(SamVariationScanCommand.class);
		registerModulePluginCmdClass(SamNucleotideCommand.class);
		registerModulePluginCmdClass(SamDepthCommand.class);
		registerModulePluginCmdClass(SamAminoAcidCommand.class);
		registerModulePluginCmdClass(SamNucleotideConsensusCommand.class);
		registerModulePluginCmdClass(ListSamReferenceCommand.class);
		addSimplePropertyName(MAX_LIKELIHOOD_PLACER_MODULE_NAME);
		addSimplePropertyName(MAX_LIKELIHOOD_PLACER_DISTANCE_CUTOFF);
		addSimplePropertyName(ALIGNER_MODULE_NAME);
		addSimplePropertyName(READ_LOG_INTERVAL);
		addSimplePropertyName(SAM_READER_VALIDATION_STRINGENCY);
		addSimplePropertyName(DEFAULT_MIN_DEPTH);
		addSimplePropertyName(DEFAULT_MIN_Q_SCORE);
		addSimplePropertyName(DEFAULT_MIN_MAP_Q);
		addSimplePropertyName(DEFAULT_SAM_REF_SENSE);
		
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, false);
		this.maxLikelihoodPlacerModuleName = PluginUtils.configureStringProperty(configElem, MAX_LIKELIHOOD_PLACER_MODULE_NAME, false);
		this.maxLikelihoodPlacerDistanceCutoff = PluginUtils.configureDoubleProperty(configElem, MAX_LIKELIHOOD_PLACER_DISTANCE_CUTOFF, 0.75);
		this.readLogInterval = Optional.ofNullable(
				PluginUtils.configureIntProperty(configElem, READ_LOG_INTERVAL, false)).orElse(20000);
		this.samReaderValidationStringency = PluginUtils.configureEnumProperty(ValidationStringency.class, configElem, SAM_READER_VALIDATION_STRINGENCY, null);
		this.defaultMinQScore = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, DEFAULT_MIN_Q_SCORE, 0, true, 99, true, false)).orElse(0);
		this.defaultMinMapQ = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, DEFAULT_MIN_MAP_Q, 0, true, 99, true, false)).orElse(0);
		this.defaultMinDepth = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, DEFAULT_MIN_DEPTH, 0, true, null, false, false)).orElse(0);
		this.defaultSamRefSense = Optional.ofNullable(PluginUtils.configureEnumProperty(SamRefSense.class, configElem, DEFAULT_SAM_REF_SENSE, false)).orElse(SamRefSense.FORWARD);
	}

	public String getAlignerModuleName() {
		return alignerModuleName;
	}

	public String getMaxLikelihoodPlacerModuleName() {
		return maxLikelihoodPlacerModuleName;
	}

	public ValidationStringency getSamReaderValidationStringency() {
		return samReaderValidationStringency;
	}

	public String establishTargetRefName(CommandContext cmdContext, String samRefName, String definedTargetRefName) {
		if(definedTargetRefName != null) {
			return definedTargetRefName;
		}
		throw new SamReporterCommandException(Code.NO_TARGET_REFERENCE_DEFINED);
	}
	
	public AlignmentMember establishTargetRefMemberUsingPlacer(CommandContext cmdContext, DNASequence consensusSequence) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = Module.resolveModulePlugin(cmdContext, MaxLikelihoodPlacer.class, maxLikelihoodPlacerModuleName);
		
		Map<String, DNASequence> consensusSequenceMap = new LinkedHashMap<String, DNASequence>();
		String key = "consensusSequence";
		consensusSequenceMap.put(key, consensusSequence);
		
		PhyloTree glueProjectPhyloTree = maxLikelihoodPlacer.constructGlueProjectPhyloTree(cmdContext);
		
		PlacerResultInternal placerResult = maxLikelihoodPlacer.place(cmdContext, glueProjectPhyloTree, consensusSequenceMap, null);
		MaxLikelihoodSingleQueryResult singleQueryResult = placerResult.getQueryResults().get(key);
		if(singleQueryResult.singlePlacement.size() == 0) {
			throw new SamReporterCommandException(Code.NO_CONSENSUS_PLACEMENTS);
		}
		MaxLikelihoodSinglePlacement firstPlacement = singleQueryResult.singlePlacement.get(0);
		
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = 
				MaxLikelihoodPlacer.generateEdgeIndexToPhyloBranch(placerResult.getLabelledPhyloTree(), glueProjectPhyloTree);

		PhyloLeaf placementLeaf = MaxLikelihoodPlacer.addPlacementToPhylogeny(glueProjectPhyloTree, edgeIndexToPhyloBranch, singleQueryResult, firstPlacement);
		
		BigDecimal distanceCutoff = new BigDecimal(maxLikelihoodPlacerDistanceCutoff); 
		List<PlacementNeighbour> placementNeighbours = PlacementNeighbourFinder.findNeighbours(placementLeaf, distanceCutoff, 1);
		if(placementNeighbours.isEmpty()) {
			throw new SamReporterCommandException(Code.NO_PLACEMENT_NEIGHBOURS_FOUND, Double.toString(maxLikelihoodPlacerDistanceCutoff));
		}
		PlacementNeighbour nearestNeighbour = placementNeighbours.get(0);
		String neighbourLeafName = nearestNeighbour.getPhyloLeaf().getName();
		Map<String,String> neighbourMemberPkMap = PhyloImporter.memberLeafNodeNameToPkMap(neighbourLeafName);
		return GlueDataObject.lookup(cmdContext, AlignmentMember.class, neighbourMemberPkMap);
	}
	
	
	
	
	
	public class RecordsCounter {
		int numRecords = 0;
		public void processedRecord() {
			numRecords++;
		}
		public void logRecordsProcessed() {
			if(numRecords % readLogInterval == 0) {
				log(Level.FINE, "Processed "+numRecords+" reads");
			}
		}
		public void logTotalRecordsProcessed() {
			log(Level.FINE, "Total reads processed: "+numRecords);
		}
	}

	public List<QueryAlignedSegment> getReadToSamRefSegs(SAMRecord samRecord) {
		List<QueryAlignedSegment> readToSamRefSegs = new ArrayList<QueryAlignedSegment>();
		samRecord.getAlignmentBlocks().forEach(almtBlock -> {
			int samRefStart = almtBlock.getReferenceStart();
			int samRefEnd = samRefStart + almtBlock.getLength()-1;
			int readStart = almtBlock.getReadStart();
			int readEnd = readStart + almtBlock.getLength()-1;
			readToSamRefSegs.add(new QueryAlignedSegment(samRefStart, samRefEnd, readStart, readEnd));
		});
		return readToSamRefSegs;
	}

	public int getDefaultMinQScore() {
		return defaultMinQScore;
	}

	public int getDefaultMinMapQ() {
		return defaultMinMapQ;
	}

	public int getDefaultMinDepth() {
		return defaultMinDepth;
	}
	
	public SamRefSense getDefaultSamRefSense() {
		return defaultSamRefSense;
	}
	
}
