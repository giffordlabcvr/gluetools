package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.navigation.NavigationDirection;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.navigation.NavigationDirection.Condition;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class FastaNtAlignmentImporter<I extends FastaNtAlignmentImporter<I>> extends BaseFastaAlignmentImporter<I> {

	public static final String MIN_ROW_COVERAGE_PERCENT = "minRowCoveragePercent";
	public static final String MIN_ROW_CORRECT_PERCENT = "minRowCorrectPercent";
	public static final String NAVIGATION_DIRECTION = "navigationDirection";



	private Double minRowCoveragePercent;
	private Double minRowCorrectPercent;
	
	// These navigation directions allow us to use an existing constrained alignment to "navigate" to the correct 
	// part of the genome when importing the unconstrained alignment.
	// Example: importing an alignment of 3'LTRs for retroviruses
	// The problem is that for a given imported alignment row, the 5'LTR of the underlying sequence may be identical.
	// So we only want to allow matches in the 3' region. This can be done by "navigating" to the region after the POL gene.
	// This is possible if we already have a constrained "navigation alignment" which
	// (a) has a reference which defines a feature location for POL.
	// (b) contains the sequence of interest as a member.
	// (c) this member has aligned segments which cover POL.
	
	private List<NavigationDirection> navigationDirections;	



	public FastaNtAlignmentImporter() {
		super();
		addSimplePropertyName(MIN_ROW_CORRECT_PERCENT);
		addSimplePropertyName(MIN_ROW_COVERAGE_PERCENT);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minRowCoveragePercent = Optional
				.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_ROW_COVERAGE_PERCENT, false))
				.orElse(95.0);
		minRowCorrectPercent = Optional
				.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_ROW_CORRECT_PERCENT, false))
				.orElse(95.0);
		List<Element> navDirectionElems = PluginUtils.findConfigElements(configElem, NAVIGATION_DIRECTION, null, null);
		this.navigationDirections = PluginFactory.createPlugins(pluginConfigContext, NavigationDirection.class, navDirectionElems);
	}

	public List<NavigationDirection> getNavigationDirections() {
		return navigationDirections;
	}
	
	public FastaAlignmentImporterResult doPreview(ConsoleCommandContext cmdContext, String fileName, String sourceName, Alignment navAlignment) {
		return doImport(cmdContext, fileName, null, sourceName, navAlignment);
	}
	
	public final FastaAlignmentImporterResult doImport(ConsoleCommandContext cmdContext, String fileName, 
			Alignment alignment, String sourceName, Alignment navAlignment) {

		List<NavigationDirection> navigationDirections = getNavigationDirections();
		Map<NavigationDirection, FeatureLocation> navDirectionToFLoc = new LinkedHashMap<NavigationDirection, FeatureLocation>(); 
		if(!navigationDirections.isEmpty()) {
			if(navAlignment == null) {
				throw new FastaAlignmentImporterException(Code.NAVIGATION_ALIGNMENT_REQUIRED);
			}
			if(!navAlignment.isConstrained()) {
				throw new FastaAlignmentImporterException(Code.NAVIGATION_ALIGNMENT_IS_UNCONSTRAINED, navAlignment.getName());
			}
			ReferenceSequence navRefSeq = navAlignment.getRefSequence();
			for(NavigationDirection navDirection: navigationDirections) {
				FeatureLocation navDirFLoc = GlueDataObject.lookup(cmdContext, 
						FeatureLocation.class, FeatureLocation.pkMap(navRefSeq.getName(), navDirection.getFeatureName()), true);
				if(navDirFLoc == null) {
					throw new FastaAlignmentImporterException(Code.NAVIGATION_REF_SEQ_FEATURE_MISSING, navRefSeq.getName(), navAlignment.getName(), navDirection.getFeatureName());
				}
				if(navDirFLoc.getSegments().isEmpty()) {
					throw new FastaAlignmentImporterException(Code.NAVIGATION_REF_SEQ_FEATURE_HAS_NO_SEGMENTS, navRefSeq.getName(), navAlignment.getName(), navDirection.getFeatureName());
				}
				navDirectionToFLoc.put(navDirection, navDirFLoc);
			}
		}
		
		byte[] fastaFileBytes = cmdContext.loadBytes(fileName);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaFileBytes);
		
		Map<String, DNASequence> sequenceMap = FastaUtils.parseFasta(fastaFileBytes);
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		int alignmentRows = 0;
		
		for(Map.Entry<String, DNASequence> entry: sequenceMap.entrySet()) {
			String fastaID = entry.getKey();

			Sequence foundSequence = findSequence(cmdContext, fastaID, sourceName);
			if(foundSequence == null) {
				continue;
			}
			
			String memberSourceName = foundSequence.getSource().getName();
			String memberSequenceID = foundSequence.getSequenceID();

			this.log(Level.FINEST, "Fasta ID "+fastaID+" was mapped to sequence "+memberSourceName+"/"+memberSequenceID);

			
			List<ReferenceSegment> foundSequenceNavigationRegion = getNavigationRegion(cmdContext, navAlignment, navDirectionToFLoc, foundSequence);
			
			AlignmentMember almtMember = null; 
			List<QueryAlignedSegment> existingSegs = null;
			if(alignment != null) {
				almtMember = ensureAlignmentMember(cmdContext, alignment, foundSequence);
				existingSegs = almtMember.getAlignedSegments().stream()
						.map(AlignedSegment::asQueryAlignedSegment)
						.collect(Collectors.toList());
			} else {
				existingSegs = new ArrayList<QueryAlignedSegment>();
			}

			List<QueryAlignedSegment> queryAlignedSegs = null; 

			DNASequence alignmentRowDnaSequence = entry.getValue();
			String alignmentRowAsString = alignmentRowDnaSequence.getSequenceAsString();
			queryAlignedSegs = findAlignedSegs(cmdContext, foundSequence, existingSegs, alignmentRowAsString, foundSequenceNavigationRegion);
			if(queryAlignedSegs == null) {
				// null return value means skip this alignment row. A warning should log the reason.
				this.log(Level.FINEST, "Alignment row skipped for fasta ID "+fastaID);
				
				continue;
			}

			AbstractSequenceObject foundSeqObj = foundSequence.getSequenceObject();

			int alignmentRowNonGapNTs = 0;
			for(int i = 0; i < alignmentRowAsString.length(); i++) {
				if(alignmentRowAsString.charAt(i) != '-') {
					alignmentRowNonGapNTs++;
				}
			}
			int coveredAlignmentRowNTs = 0;
			
			int correctCalls = 0;
			for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
				coveredAlignmentRowNTs += queryAlignedSeg.getCurrentLength();
				int queryStart = queryAlignedSeg.getQueryStart();
				for(int i = 0; i < queryAlignedSeg.getCurrentLength(); i++) {
					char almtRowNT = alignmentRowAsString.charAt((queryAlignedSeg.getRefStart()+i)-1);
					char foundNT = foundSeqObj.getNucleotides(cmdContext, queryStart+i, queryStart+i).charAt(0); 
					if(almtRowNT == foundNT) {
						correctCalls++;
					} 
				}
			}
			double alignmentRowCoveragePct = 0.0;
			double correctCallsPct = 0.0;
			if(alignmentRowNonGapNTs > 0) {
				alignmentRowCoveragePct = 100.0 * coveredAlignmentRowNTs / (double) alignmentRowNonGapNTs;
				correctCallsPct = 100.0 * correctCalls / (double) coveredAlignmentRowNTs;
			}
			
			if(alignmentRowCoveragePct < minRowCoveragePercent) {
				GlueLogger.getGlueLogger().warning("Skipping row with fasta ID "+fastaID+" row coverage percent "+alignmentRowCoveragePct+" < "+minRowCoveragePercent);
				continue;
			}
			if(correctCallsPct < minRowCorrectPercent) {
				GlueLogger.getGlueLogger().warning("Skipping row with fasta ID "+fastaID+" correct calls percent "+correctCallsPct+" < "+minRowCorrectPercent);
				continue;
			}
			
			if(alignment != null) {
				for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
					AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, 
							AlignedSegment.pkMap(alignment.getName(), memberSourceName, memberSequenceID, 
									queryAlignedSeg.getRefStart(), queryAlignedSeg.getRefEnd(), 
									queryAlignedSeg.getQueryStart(), queryAlignedSeg.getQueryEnd()), false);
					alignedSegment.setAlignmentMember(almtMember);
				}
			}
			
			Map<String, Object> memberResultMap = new LinkedHashMap<String, Object>();
			memberResultMap.put("fastaID", fastaID);
			memberResultMap.put("sourceName", memberSourceName);
			memberResultMap.put("sequenceID", memberSequenceID);
			memberResultMap.put("numSegmentsAdded", new Integer(queryAlignedSegs.size()));
			memberResultMap.put("almtRowCoverage", alignmentRowCoveragePct);
			memberResultMap.put("correctCalls", correctCallsPct);
			resultListOfMaps.add(memberResultMap);
			alignmentRows++;
			if(alignmentRows % 25 == 0) {
				log("Imported "+alignmentRows+" alignment rows");
			}
			
		}
		log("Imported "+alignmentRows+" alignment rows");
		
		cmdContext.commit();
		return new FastaAlignmentImporterResult(resultListOfMaps);
	}


	private List<ReferenceSegment> getNavigationRegion(ConsoleCommandContext cmdContext, Alignment navAlignment, 
			Map<NavigationDirection, FeatureLocation> navDirectionToFLoc, Sequence foundSequence) {
		List<NavigationDirection> navigationDirections = getNavigationDirections();
		int foundSeqLength = foundSequence.getSequenceObject().getNucleotides(cmdContext).length();
		List<ReferenceSegment> navigationRegion = new ArrayList<ReferenceSegment>(Arrays.asList(new ReferenceSegment(1, foundSeqLength)));
		if(navigationDirections.isEmpty()) {
			return navigationRegion;
		}
		String memberSourceName = foundSequence.getSource().getName();
		String memberSequenceID = foundSequence.getSequenceID();

		List<QueryAlignedSegment> foundSeqToRefSegs = null;

		Sequence refSeqSeq = navAlignment.getRefSequence().getSequence();
		if(refSeqSeq.getSource().getName().equals(foundSequence.getSource().getName()) && 
				refSeqSeq.getSequenceID().equals(foundSequence.getSequenceID())) {
			foundSeqToRefSegs = Arrays.asList(new QueryAlignedSegment(1, foundSeqLength, 1, foundSeqLength));
		} else {
			AlignmentMember navAlignmentMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
					AlignmentMember.pkMap(navAlignment.getName(), memberSourceName, memberSequenceID), true);
			if(navAlignmentMember == null) {
				throw new FastaAlignmentImporterException(Code.NAVIGATION_ALIGNMENT_MEMBER_NOT_FOUND, navAlignment.getName(), memberSourceName, memberSequenceID);
			}
			foundSeqToRefSegs = navAlignmentMember.getAlignedSegments().stream().map(alSeg -> alSeg.asQueryAlignedSegment()).collect(Collectors.toList());
		}
		
		for(NavigationDirection navDirection: navigationDirections) {
			Condition condition = navDirection.getCondition();
			String featureName = navDirection.getFeatureName();
			FeatureLocation navDirectionFLoc = navDirectionToFLoc.get(navDirection);
			List<ReferenceSegment> navFLocRefSegs = navDirectionFLoc.segmentsAsReferenceSegments();
			List<QueryAlignedSegment> memberToRefFlocSegs = ReferenceSegment.intersection(foundSeqToRefSegs, navFLocRefSegs, ReferenceSegment.cloneLeftSegMerger());
			if(memberToRefFlocSegs.isEmpty()) {
				throw new FastaAlignmentImporterException(Code.NAVIGATION_ALIGNMENT_MEMBER_DOES_NOT_COVER_FEATURE, navAlignment.getName(), memberSourceName, memberSequenceID, featureName);
			}
			Integer memberFeatureQueryStart = QueryAlignedSegment.minQueryStart(memberToRefFlocSegs);
			Integer memberFeatureQueryEnd = QueryAlignedSegment.maxQueryEnd(memberToRefFlocSegs);
			switch(condition) {
			case BEFORE_START:
				if(memberFeatureQueryStart.equals(1)) {
					navigationRegion.clear();
				} else {
					navigationRegion = ReferenceSegment.intersection(navigationRegion, Arrays.asList(new ReferenceSegment(1, memberFeatureQueryStart-1)), ReferenceSegment.cloneLeftSegMerger());
				}
				break;
			case AFTER_END:
				if(memberFeatureQueryEnd.equals(foundSeqLength)) {
					navigationRegion.clear();
				} else {
					navigationRegion = ReferenceSegment.intersection(navigationRegion, Arrays.asList(new ReferenceSegment(memberFeatureQueryEnd+1, foundSeqLength)), ReferenceSegment.cloneLeftSegMerger());
				}
				break;
			default:
				throw new RuntimeException("Unknown navigation condition "+condition.name());
			}
		}
		return navigationRegion;
	}

	public abstract List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext, Sequence foundSequence, 
			List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs, 
			List<ReferenceSegment> foundSequenceNavigationRegion);

	public List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext,
			Sequence foundSequence, List<QueryAlignedSegment> existingSegs,
			String fastaAlignmentNTs) {
		int foundSeqLength = foundSequence.getSequenceObject().getNucleotides(cmdContext).length();
		List<ReferenceSegment> navigationRegion = new ArrayList<ReferenceSegment>(Arrays.asList(new ReferenceSegment(1, foundSeqLength)));
		return findAlignedSegs(cmdContext, foundSequence, existingSegs, fastaAlignmentNTs, navigationRegion);
	}


}
